package no.difi.meldingsutveksling.status;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.StatusStrategy;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
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
    private final ObjectProvider<StatusStrategy> statusStrategyProvider;
    @Getter(lazy = true) private final Map<ServiceIdentifier, StatusStrategy> statusStrategyMap = createStatusStrategyMap();

    private static final Set<ReceiptStatus> COMPLETABLES = Sets.newHashSet(LEST, FEIL, LEVETID_UTLOPT, INNKOMMENDE_LEVERT);

    private Map<ServiceIdentifier, StatusStrategy> createStatusStrategyMap() {
        return statusStrategyProvider.stream().collect(
                Collectors.toConcurrentMap(StatusStrategy::getServiceIdentifier, Function.identity())
        );
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
    public Optional<Conversation> registerStatus(@NotNull String messageId, @NotNull ReceiptStatus... status) {
        for (ReceiptStatus s : status) {
            registerStatus(messageId, messageStatusFactory.getMessageStatus(s));
        }
        return repo.findByMessageId(messageId).stream().findFirst();
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

        getStatusStrategy(status)
                .ifPresent(statusStrategy -> {
                    if (statusStrategy.isStartPolling(status)) {
                        conversation.setPollable(true);
                    } else if (statusStrategy.isStopPolling(status)) {
                        conversation.setPollable(false);
                    }
                });

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

    private Optional<StatusStrategy> getStatusStrategy(MessageStatus status) {
        Conversation conversation = status.getConversation();

        if (conversation.getDirection() != ConversationDirection.OUTGOING) {
            return Optional.empty();
        }

        return Optional.ofNullable(getStatusStrategyMap().get(conversation.getServiceIdentifier()));
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
        OffsetDateTime ttl = sbd.getExpectedResponseDateTime()
                .orElse(OffsetDateTime.now(clock).plusHours(props.getNextmove().getDefaultTtlHours()));

        return registerConversation(new MessageInformable() {
            @Override
            public String getConversationId() {
                return sbd.getConversationId();
            }

            @Override
            public String getMessageId() {
                return sbd.getMessageId();
            }

            @Override
            public PartnerIdentifier getSender() {
                return sbd.getSenderIdentifier();
            }

            @Override
            public PartnerIdentifier getReceiver() {
                return sbd.getReceiverIdentifier();
            }

            @Override
            public String getProcessIdentifier() {
                return sbd.getProcess();
            }

            @Override
            public String getDocumentIdentifier() {
                return sbd.getDocumentType();
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
    public Optional<Conversation> findConversation(@NotNull String conversationId, @NotNull ConversationDirection
            direction) {
        return repo.findByConversationIdAndDirection(conversationId, direction);
    }
}
