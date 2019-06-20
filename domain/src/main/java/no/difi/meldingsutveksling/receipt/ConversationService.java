package no.difi.meldingsutveksling.receipt;

import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.webhooks.WebhookPublisher;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPF;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository repo;
    private final IntegrasjonspunktProperties props;
    private final WebhookPublisher webhookPublisher;
    private final MessageStatusFactory messageStatusFactory;
    private final Clock clock;

    private static final String CONVERSATION_EXISTS = "Conversation with id=%s already exists, not recreating";
    private static final Set<ServiceIdentifier> POLLABLES = Sets.newHashSet(DPV, DPF);

    @Transactional
    public Optional<Conversation> registerStatus(String conversationId, MessageStatus status) {
        Optional<Conversation> c = repo.findByConversationId(conversationId).stream().findFirst();
        if (c.isPresent()) {
            return Optional.of(registerStatus(c.get(), status));
        } else {
            log.warn(format("Conversation with id=%s not found, cannot register receipt status=%s", conversationId, status));
            return Optional.empty();
        }
    }

    @SuppressWarnings("squid:S2250")
    @Transactional
    public Conversation registerStatus(Conversation conversation, MessageStatus status) {
        if (conversation.hasStatus(status)) {
            return conversation;
        }

        conversation.addMessageStatus(status)
                .setLastUpdate(OffsetDateTime.now(clock));
        if (isPollable(conversation, status)) {
            // Note: isPollable can not be moved into setPollable, as this would interrupt polling
            // for every other registered status than 'SENDT'
            conversation.setPollable(true);
        }
        if (ReceiptStatus.valueOf(status.getStatus()) == LEVERT) {
            markFinished(conversation);
        }
        if (asList(LEST, FEIL, LEVETID_UTLOPT, INNKOMMENDE_LEVERT).contains(ReceiptStatus.valueOf(status.getStatus()))) {
            markFinished(conversation);
            disablePolling(conversation);
        }

        webhookPublisher.publish(conversation, status);

        log.debug(String.format("Added status '%s' to conversation[id=%s]", status.getStatus(),
                conversation.getConversationId()),
                MessageStatusMarker.from(status));

        return repo.save(conversation);
    }

    private boolean isPollable(Conversation conversation, MessageStatus status) {
        return conversation.getDirection() == ConversationDirection.OUTGOING &&
                ReceiptStatus.SENDT.toString().equals(status.getStatus()) &&
                POLLABLES.contains(conversation.getServiceIdentifier());
    }

    @Transactional
    public Conversation markFinished(Conversation conversation) {
        return repo.save(conversation
                .setFinished(true));
    }

    @Transactional
    public Conversation disablePolling(Conversation conversation) {
        return repo.save(conversation.setPollable(false));
    }

    @Transactional
    public Conversation save(Conversation conversation) {
        return repo.save(conversation);
    }

    @Transactional
    public Conversation registerConversation(MessageInformable message) {
        return findConversation(message.getConversationId())
                .orElseGet(() -> createConversation(message));
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
            public String getSenderIdentifier() {
                return sbd.getSenderIdentifier();
            }

            @Override
            public String getReceiverIdentifier() {
                return sbd.getReceiverIdentifier();
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

    public Optional<Conversation> findConversation(String conversationId) {
        return repo.findByConversationId(conversationId).stream()
                .findFirst()
                .filter(p -> {
                    log.warn(String.format(CONVERSATION_EXISTS, conversationId));
                    return true;
                });
    }

    private Conversation createConversation(MessageInformable message) {
        MessageStatus ms = messageStatusFactory.getMessageStatus(ReceiptStatus.OPPRETTET);
        Conversation c = Conversation.of(message, OffsetDateTime.now(clock), ms);
        webhookPublisher.publish(c, ms);
        return repo.save(c);
    }
}
