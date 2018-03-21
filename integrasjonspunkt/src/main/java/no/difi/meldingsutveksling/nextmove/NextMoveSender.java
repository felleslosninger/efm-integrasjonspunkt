package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Optional;

import static no.difi.meldingsutveksling.nextmove.ConversationDirection.OUTGOING;
import static no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers.markerFrom;

@Component
@Slf4j
public class NextMoveSender {

    private ConversationStrategyFactory strategyFactory;
    private ConversationService conversationService;
    private MessagePersister messagePersister;
    private DirectionalConversationResourceRepository outRepo;

    @Autowired
    public NextMoveSender(ConversationStrategyFactory strategyFactory,
                          ConversationService conversationService,
                          MessagePersister messagePersister,
                          ConversationResourceRepository repo) {
        this.strategyFactory = strategyFactory;
        this.conversationService = conversationService;
        this.messagePersister = messagePersister;
        this.outRepo = new DirectionalConversationResourceRepository(repo, OUTGOING);
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
        conversationService.registerStatus(cr.getConversationId(), MessageStatus.of(GenericReceiptStatus.SENDT));
        outRepo.delete(cr);
        try {
            messagePersister.delete(cr);
        } catch (IOException e) {
            log.error("Error deleting files from conversation with id={}", cr.getConversationId(),  e);
        }
    }
}
