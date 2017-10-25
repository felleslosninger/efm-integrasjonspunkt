package no.difi.meldingsutveksling.receipt;

import lombok.extern.log4j.Log4j;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.lang.String.format;

@Component
@Log4j
public class ConversationService {

    private ConversationRepository repo;

    @Autowired
    public ConversationService(ConversationRepository repo) {
        this.repo = repo;
    }

    public void registerStatus(String conversationId, MessageStatus status) {
        Optional<Conversation> c = repo.findByConversationId(conversationId).stream().findFirst();
        if (c.isPresent()) {
            registerStatus(c.get(), status);
        } else {
            log.warn(format("Conversation with id=%s not found, cannot register receipt status=%s", conversationId, status));
        }
    }

    public void registerStatus(Conversation conversation, MessageStatus status) {
        boolean hasStatus = conversation.getMessageStatuses().stream()
                .map(MessageStatus::getStatus)
                .anyMatch(status.getStatus()::equals);
        if (!hasStatus) {
            conversation.addMessageStatus(status);
            repo.save(conversation);
        }
    }

    public void markFinished(String conversationId) {
        Optional<Conversation> c = repo.findByConversationId(conversationId).stream().findFirst();
        c.ifPresent(this::markFinished);
    }

    public void markFinished(Conversation conversation) {
        conversation.setFinished(true);
        conversation.setPollable(false);
        repo.save(conversation);
    }

    public void registerConversation(EDUCore message) {
        MessageStatus ms = MessageStatus.of(GenericReceiptStatus.OPPRETTET);
        if (message.getMessageType() == EDUCore.MessageType.APPRECEIPT) {
            ms.setDescription("AppReceipt");
        }
        Conversation conversation = Conversation.of(message, ms);
        repo.save(conversation);
    }

    public Conversation registerConversation(ConversationResource cr) {
        Optional<Conversation> find = repo.findByConversationId(cr.getConversationId()).stream().findFirst();
        if (find.isPresent()) {
            log.warn(String.format("Conversation with id=%s already exists, not recreating", cr.getConversationId()));
            return find.get();
        }
        MessageStatus ms = MessageStatus.of(GenericReceiptStatus.OPPRETTET);
        Conversation c = Conversation.of(cr, ms);
        return repo.save(c);
    }

}
