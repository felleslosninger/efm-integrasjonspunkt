package no.difi.meldingsutveksling.status;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentUtils;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.mail.IpMailSender;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.receipt.StatusQueue;
import no.difi.meldingsutveksling.webhooks.WebhookPublisher;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static no.difi.meldingsutveksling.ServiceIdentifier.*;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.INCOMING;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.*;


@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultConversationService implements ConversationService {

    private final ConversationRepository repo;
    private final IntegrasjonspunktProperties props;
    private final WebhookPublisher webhookPublisher;
    private final MessageStatusFactory messageStatusFactory;
    private final IpMailSender ipMailSender;
    private final Clock clock;
    private final StatusQueue statusQueue;
    private final ObjectProvider<MeldingsformidlerClient> meldingsformidlerClientObjectProvider;
    @Getter(lazy = true) private final Set<ServiceIdentifier> pollables = createrPollables();

    private static final Set<ReceiptStatus> COMPLETABLES = Sets.newHashSet(LEST, FEIL, LEVETID_UTLOPT, INNKOMMENDE_LEVERT);

    private Set<ServiceIdentifier> createrPollables() {
        Set<ServiceIdentifier> serviceIdentifiers = new HashSet<>();
        serviceIdentifiers.add(DPV);
        serviceIdentifiers.add(DPF);


        if (meldingsformidlerClientObjectProvider.stream()
                .anyMatch(MeldingsformidlerClient::skalPolleMeldingStatus)) {
            serviceIdentifiers.add(DPI);
        }

        return Collections.unmodifiableSet(serviceIdentifiers);
    }

    @NotNull
    @Transactional
    public Optional<Conversation> registerStatus(@NotNull String messageId, @NotNull MessageStatus status) {
        Optional<Conversation> c = repo.findByMessageId(messageId).stream().findFirst();
        if (c.isPresent()) {
            return Optional.of(registerStatus(c.get(), status));
        } else {
            log.warn(format("Conversation with id=%s not found, cannot register receipt status=%s", messageId, status));
            return Optional.empty();
        }
    }

    @NotNull
    @Override
    @Transactional
    public Optional<Conversation> registerStatus(@NotNull String messageId, @NotNull ReceiptStatus status) {
        return registerStatus(messageId, messageStatusFactory.getMessageStatus(status));
    }

    @NotNull
    @Override
    @Transactional
    public Optional<Conversation> registerStatus(@NotNull String messageId, @NotNull ReceiptStatus status, @NotNull String description) {
        return registerStatus(messageId, messageStatusFactory.getMessageStatus(status, description));
    }

    @NotNull
    @SuppressWarnings("squid:S2250")
    @Transactional
    public Conversation registerStatus(Conversation conversation, @NotNull MessageStatus status) {
        MDC.put(NextMoveConsts.CORRELATION_ID, conversation.getMessageId());
        if (conversation.hasStatus(status)) {
            return conversation;
        }

        conversation.addMessageStatus(status);

        if (isPollable(status)) {
            // Note: isPollable can not be moved into setPollable, as this would interrupt polling
            // for every other registered status than 'SENDT'
            conversation.setPollable(true);
        }
        if (ReceiptStatus.valueOf(status.getStatus()) == LEVERT) {
            conversation.setFinished(true);
        }
        if (ReceiptStatus.valueOf(status.getStatus()) == FEIL &&
                props.getFeature().isMailErrorStatus()) {
            trySendMail(conversation);
        }
        if (COMPLETABLES.contains(ReceiptStatus.valueOf(status.getStatus()))) {
            conversation.setFinished(true)
                    .setPollable(false);
        }

        log.debug(String.format("Added status '%s' to conversation[id=%s]", status.getStatus(),
                        conversation.getMessageId()),
                MessageStatusMarker.from(status));
        repo.save(conversation);
        webhookPublisher.publish(conversation, status);
        statusQueue.enqueueStatus(status, conversation);

        return conversation;
    }

    private void trySendMail(Conversation conversation) {
        try {
            String title = format("Integrasjonspunkt: status %s registrert for forsendelse %s", FEIL.toString(), conversation.getMessageId());
            title = (isNullOrEmpty(conversation.getMessageReference())) ? title : title + format(" / %s", conversation.getMessageReference());
            String direction = conversation.getDirection() == INCOMING ? "Innkommende" : "Utgående";
            String messageRef = (isNullOrEmpty(conversation.getMessageReference())) ? "" : format("og messageReference %s ", conversation.getMessageReference());
            String body = format("%s forsendelse med conversationId %s (messageId %s) %shar registrert status '%s'. Se statusgrensesnitt for detaljer.",
                    direction, conversation.getConversationId(), conversation.getMessageId(), messageRef, FEIL.toString());
            ipMailSender.send(title, body);
        } catch (Exception e) {
            log.error(format("Error sending status mail for messageId %s", conversation.getMessageId()), e);
        }
    }

    private boolean isPollable(MessageStatus status) {
        Conversation conversation = status.getConversation();
        return conversation.getDirection() == ConversationDirection.OUTGOING &&
                ReceiptStatus.SENDT.toString().equals(status.getStatus()) &&
                getPollables().contains(conversation.getServiceIdentifier());
    }

    @NotNull
    @Transactional
    public Conversation save(@NotNull Conversation conversation) {
        return repo.save(conversation);
    }

    @NotNull
    @Transactional
    public Conversation registerConversation(MessageInformable message, ReceiptStatus... statuses) {
        Conversation c = findConversation(message.getMessageId()).filter(p -> {
            log.debug(format("Conversation with id=%s already exists, not recreating", message.getMessageId()));
            return true;
        }).orElseGet(() -> createConversation(message));
        for (ReceiptStatus s : statuses) {
            registerStatus(c, messageStatusFactory.getMessageStatus(s));
        }
        return c;
    }

    @NotNull
    @Override
    @Transactional
    public Conversation registerConversation(StandardBusinessDocument sbd,
                                             @NotNull ServiceIdentifier si,
                                             @NotNull ConversationDirection conversationDirection,
                                             @NotNull ReceiptStatus... statuses) {
        OffsetDateTime ttl = StandardBusinessDocumentUtils.getExpectedResponseDateTime(sbd)
                .orElse(OffsetDateTime.now(clock).plusHours(props.getNextmove().getDefaultTtlHours()));

        return registerConversation(new MessageInformable() {
            @Override
            public String getConversationId() {
                return SBDUtil.getConversationId(sbd);
            }

            @Override
            public String getMessageId() {
                return SBDUtil.getMessageId(sbd);
            }

            @Override
            public PartnerIdentifier getSender() {
                return SBDUtil.getSender(sbd);
            }

            @Override
            public PartnerIdentifier getReceiver() {
                return SBDUtil.getReceiver(sbd);
            }

            @Override
            public String getProcessIdentifier() {
                return SBDUtil.getProcess(sbd);
            }

            @Override
            public String getDocumentIdentifier() {
                return SBDUtil.getDocumentType(sbd);
            }

            @Override
            public ConversationDirection getDirection() {
                return conversationDirection;
            }

            @Override
            public ServiceIdentifier getServiceIdentifier() {
                return si;
            }

            @Override
            public OffsetDateTime getExpiry() {
                return ttl;
            }
        }, statuses);
    }

    @NotNull
    public Optional<Conversation> findConversation(@NotNull String messageId) {
        return repo.findByMessageId(messageId).stream().findFirst();
    }

    Conversation createConversation(MessageInformable message) {
        MessageStatus ms = messageStatusFactory.getMessageStatus(ReceiptStatus.OPPRETTET);
        Conversation c = Conversation.of(message, OffsetDateTime.now(clock), ms);
        repo.save(c);
        webhookPublisher.publish(c, ms);
        statusQueue.enqueueStatus(ms, c);
        return c;
    }

    @NotNull
    @Override
    public Optional<Conversation> findConversation(@NotNull String conversationId, @NotNull ConversationDirection direction) {
        return repo.findByConversationIdAndDirection(conversationId, direction);
    }
}
