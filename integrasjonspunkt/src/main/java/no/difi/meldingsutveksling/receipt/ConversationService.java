package no.difi.meldingsutveksling.receipt;

import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.mail.MailSender;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.webhooks.WebhookPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPF;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.INCOMING;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.*;


@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository repo;
    private final IntegrasjonspunktProperties props;
    private final WebhookPublisher webhookPublisher;
    private final MessageStatusFactory messageStatusFactory;
    private final MailSender mailSender;
    private final Clock clock;
    private final StatusQueue statusQueue;

    private static final Set<ServiceIdentifier> POLLABLES = Sets.newHashSet(DPV, DPF);
    private static final Set<ReceiptStatus> COMPLETABLES = Sets.newHashSet(LEST, FEIL, LEVETID_UTLOPT, INNKOMMENDE_LEVERT);

    @Transactional
    public Optional<Conversation> registerStatus(String messageId, MessageStatus status) {
        Optional<Conversation> c = repo.findByMessageId(messageId).stream().findFirst();
        if (c.isPresent()) {
            return Optional.of(registerStatus(c.get(), status));
        } else {
            log.warn(format("Conversation with id=%s not found, cannot register receipt status=%s", messageId, status));
            return Optional.empty();
        }
    }

    @SuppressWarnings("squid:S2250")
    @Transactional
    public Conversation registerStatus(Conversation conversation, MessageStatus status) {
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
            mailSender.send(title, body);
        } catch (Exception e) {
            log.error(format("Error sending status mail for messageId %s", conversation.getMessageId()), e);
        }
    }

    private boolean isPollable(MessageStatus status) {
        Conversation conversation = status.getConversation();
        return conversation.getDirection() == ConversationDirection.OUTGOING &&
                ReceiptStatus.SENDT.toString().equals(status.getStatus()) &&
                POLLABLES.contains(conversation.getServiceIdentifier());
    }

    @Transactional
    public Conversation save(Conversation conversation) {
        return repo.save(conversation);
    }

    @Transactional
    public Conversation registerConversation(MessageInformable message) {
        return findConversation(message.getMessageId()).filter(p -> {
            log.debug(String.format("Conversation with id=%s already exists, not recreating", message.getMessageId()));
            return true;
        }).orElseGet(() -> createConversation(message));
    }

    @Transactional
    public Conversation registerConversation(StandardBusinessDocument sbd, ServiceIdentifier si, ConversationDirection conversationDirection) {
        OffsetDateTime ttl = sbd.getExpectedResponseDateTime().orElse(OffsetDateTime.now(clock).plusHours(props.getNextmove().getDefaultTtlHours()));
        return registerConversation(new MessageInformable() {
            @Override
            public String getConversationId() {
                return sbd.getConversationId();
            }

            @Override
            public String getMessageId() {
                return sbd.getDocumentId();
            }

            @Override
            public String getSenderIdentifier() {
                return sbd.getSenderIdentifier();
            }

            @Override
            public String getReceiverIdentifier() {
                return sbd.getReceiverIdentifier();
            }

            @Override
            public String getProcessIdentifier() {
                return sbd.getProcess();
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
        });
    }

    public Optional<Conversation> findConversation(String messageId) {
        return repo.findByMessageId(messageId).stream().findFirst();
    }

    private Conversation createConversation(MessageInformable message) {
        MessageStatus ms = messageStatusFactory.getMessageStatus(ReceiptStatus.OPPRETTET);
        Conversation c = Conversation.of(message, OffsetDateTime.now(clock), ms);
        repo.save(c);
        webhookPublisher.publish(c, ms);
        statusQueue.enqueueStatus(ms, c);
        return c;
    }
}
