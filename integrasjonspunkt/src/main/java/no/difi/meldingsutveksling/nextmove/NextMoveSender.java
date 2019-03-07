package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageRepository;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Optional;

import static no.difi.meldingsutveksling.nextmove.ConversationDirection.OUTGOING;
import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Component
@Slf4j
public class NextMoveSender {

    private ConversationStrategyFactory strategyFactory;
    private ConversationService conversationService;
    private MessagePersister messagePersister;
    private NextMoveMessageRepository messageRepo;
    private DirectionalConversationResourceRepository outRepo;

    public NextMoveSender(ConversationStrategyFactory strategyFactory,
                          ConversationService conversationService,
                          ObjectProvider<MessagePersister> messagePersister,
                          NextMoveMessageRepository messageRepo, ConversationResourceRepository repo) {
        this.strategyFactory = strategyFactory;
        this.conversationService = conversationService;
        this.messagePersister = messagePersister.getIfUnique();
        this.messageRepo = messageRepo;
        this.outRepo = new DirectionalConversationResourceRepository(repo, OUTGOING);
    }

    @Transactional
    public void send(NextMoveMessage msg) throws NextMoveException {
        Optional<ConversationStrategy> strategy = strategyFactory.getStrategy(msg);
        if (!strategy.isPresent()) {
            String errorStr = String.format("Cannot send message - serviceIdentifier \"%s\" not supported",
                    msg.getServiceIdentifier());
            log.error(markerFrom(msg), errorStr);
            throw new NextMoveRuntimeException(errorStr);
        }

        strategy.get().send(msg);
        if (msg.getServiceIdentifier() == ServiceIdentifier.DPE_RECEIPT) {
            return;
        }

        conversationService.registerStatus(msg.getConversationId(), MessageStatus.of(GenericReceiptStatus.SENDT));
        messageRepo.delete(msg);
        try {
            messagePersister.delete(msg.getConversationId());
        } catch (IOException e) {
            log.error("Error deleting files from conversation with id={}", msg.getConversationId(),  e);
        }
    }

    @Transactional
    public void send(ConversationResource cr) throws NextMoveException {
        Optional<ConversationStrategy> strategy = strategyFactory.getStrategy(cr);
        if (!strategy.isPresent()) {
            String errorStr = String.format("Cannot send message - serviceIdentifier \"%s\" not supported",
                    cr.getServiceIdentifier());
            log.error(markerFrom(cr), errorStr);
            throw new NextMoveRuntimeException(errorStr);
        }
        strategy.get().send(cr);
        if (cr.getServiceIdentifier() == ServiceIdentifier.DPE_RECEIPT) {
            return;
        }
        conversationService.registerStatus(cr.getConversationId(), MessageStatus.of(GenericReceiptStatus.SENDT));
        outRepo.delete(cr);
        try {
            messagePersister.delete(cr.getConversationId());
        } catch (IOException e) {
            log.error("Error deleting files from conversation with id={}", cr.getConversationId(),  e);
        }
    }
}
