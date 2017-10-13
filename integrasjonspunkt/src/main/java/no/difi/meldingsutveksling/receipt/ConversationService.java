package no.difi.meldingsutveksling.receipt;

import lombok.extern.log4j.Log4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.core.EDUCore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
        conversation.addMessageStatus(status);
        repo.save(conversation);
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
        MessageStatus ms = MessageStatus.of(GenericReceiptStatus.OPPRETTET.toString(), LocalDateTime.now());
        if (message.getMessageType() == EDUCore.MessageType.APPRECEIPT) {
            ms.setDescription("AppReceipt");
        }
        Conversation conversation = Conversation.of(message, ms);
        repo.save(conversation);
    }

    public void registerSentStatus(EDUCore message) {
        MessageStatus status = MessageStatus.of(GenericReceiptStatus.SENDT.toString(), LocalDateTime.now());
        if (message.getMessageType() == EDUCore.MessageType.APPRECEIPT) {
            status.setDescription("AppReceipt");
        }
        Optional<Conversation> conv = repo.findByConversationId(message.getId()).stream().findFirst();
        conv.ifPresent(c -> {
            c.addMessageStatus(status);
            repo.save(c);
        });
    }

    public void registerReadStatus(EDUCore message) {
        MessageStatus status = MessageStatus.of(GenericReceiptStatus.LEST.toString(), LocalDateTime.now());
        Conversation c = repo.findByConversationId(message.getId())
                .stream()
                .findFirst()
                .orElse(Conversation.of(message.getId(),
                        message.getMessageReference(),
                        message.getReceiver().getIdentifier(),
                        message.getMessageReference(),
                        ServiceIdentifier.DPO));
        c.addMessageStatus(status);
        c.setFinished(true);
        c.setPollable(false);
        repo.save(c);
    }

}
