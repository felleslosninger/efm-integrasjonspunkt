package no.difi.meldingsutveksling.receipt;

import lombok.extern.log4j.Log4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPF;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;

@Component
@Log4j
public class ConversationService {

    private ConversationRepository repo;

    private static final List<ServiceIdentifier> POLLABLES = Lists.newArrayList(DPV, DPF);

    @Autowired
    public ConversationService(ConversationRepository repo) {
        this.repo = repo;
    }

    public Optional<Conversation> registerStatus(String conversationId, MessageStatus status) {
        Optional<Conversation> c = repo.findByConversationId(conversationId).stream().findFirst();
        if (c.isPresent()) {
            return Optional.of(registerStatus(c.get(), status));
        } else {
            log.warn(format("Conversation with id=%s not found, cannot register receipt status=%s", conversationId, status));
            return Optional.empty();
        }
    }

    public Conversation registerStatus(Conversation conversation, MessageStatus status) {
        boolean hasStatus = conversation.getMessageStatuses().stream()
                .map(MessageStatus::getStatus)
                .anyMatch(status.getStatus()::equals);
        if (!hasStatus) {
            conversation.addMessageStatus(status);

            if (GenericReceiptStatus.SENDT.toString().equals(status.getStatus()) &&
                    POLLABLES.contains(conversation.getServiceIdentifier())) {
                conversation.setPollable(true);
            }
            return repo.save(conversation);
        }
        return conversation;
    }

    public Conversation markFinished(Conversation conversation) {
        conversation.setFinished(true);
        conversation.setPollable(false);
        return repo.save(conversation);
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
