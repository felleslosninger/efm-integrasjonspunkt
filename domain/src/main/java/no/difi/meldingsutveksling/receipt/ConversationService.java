package no.difi.meldingsutveksling.receipt;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.webhooks.WebhookPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.ServiceIdentifier.*;

@Component
@Slf4j
public class ConversationService {

    private final ConversationRepository repo;
    private final WebhookPublisher webhookPublisher;
    private final MessageStatusFactory messageStatusFactory;
    private final Clock clock;

    private static final String CONVERSATION_EXISTS = "Conversation with id=%s already exists, not recreating";
    private static final Set<ServiceIdentifier> POLLABLES = Sets.newHashSet(DPV, DPF, DPO);

    @Autowired
    public ConversationService(ConversationRepository repo,
                               WebhookPublisher webhookPublisher,
                               MessageStatusFactory messageStatusFactory,
                               Clock clock) {
        this.repo = repo;
        this.webhookPublisher = webhookPublisher;
        this.messageStatusFactory = messageStatusFactory;
        this.clock = clock;
    }

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
                .setPollable(isPollable(conversation, status));

        webhookPublisher.publish(status);

        Audit.info(String.format("Added status '%s' to conversation[id=%s]", status.getStatus(),
                conversation.getConversationId()),
                MessageStatusMarker.from(status));

        return repo.save(conversation);
    }

    private boolean isPollable(Conversation conversation, MessageStatus status) {
        return conversation.getDirection() == ConversationDirection.OUTGOING &&
                ReceiptStatus.SENDT.toString().equals(status.getStatus()) &&
                POLLABLES.contains(conversation.getServiceIdentifier()) &&
                !conversation.isMsh();
    }

    @Transactional
    public Conversation markFinished(Conversation conversation) {
        return repo.save(conversation
                .setFinished(true)
                .setPollable(false));
    }

    @Transactional
    public Conversation registerConversation(MessageInformable message) {
        return findConversation(message.getConversationId())
                .orElseGet(() -> createConversation(message));
    }

    public Optional<Conversation> findConversation(String conversationId) {
        return repo.findByConversationId(conversationId).stream()
                .findFirst()
                .filter(p -> {  log.warn(String.format(CONVERSATION_EXISTS, conversationId)); return true; });
    }

    private Conversation createConversation(MessageInformable message) {
        MessageStatus ms = messageStatusFactory.getMessageStatus(ReceiptStatus.OPPRETTET);
        Conversation c = Conversation.of(message, ms);
        return repo.save(c);
    }
}
