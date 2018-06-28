package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.nextmove.ConversationDirection.OUTGOING;
import static no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers.markerFrom;

@Component
@Slf4j
public class NextMoveSender {

    private ConversationStrategyFactory strategyFactory;
    private ConversationService conversationService;
    private MessagePersister messagePersister;
    private DirectionalConversationResourceRepository outRepo;
    private ServiceRegistryLookup sr;

    @Autowired
    public NextMoveSender(ConversationStrategyFactory strategyFactory,
                          ConversationService conversationService,
                          ObjectProvider<MessagePersister> messagePersister,
                          ConversationResourceRepository repo) {
        this.strategyFactory = strategyFactory;
        this.conversationService = conversationService;
        this.messagePersister = messagePersister.getIfUnique();
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

        List<ServiceRecord> serviceRecords = sr.getServiceRecords(cr.getReceiverId());
        Optional<ServiceRecord> serviceRecord = serviceRecords.stream()
                .filter(r -> cr.getServiceIdentifier() == r.getServiceIdentifier())
                .findFirst();
        if (!serviceRecord.isPresent()) {
            List<ServiceIdentifier> acceptableTypes = serviceRecords.stream()
                    .map(ServiceRecord::getServiceIdentifier)
                    .collect(Collectors.toList());
            String errorStr = String.format("Message is of type '%s', but receiver '%s' accepts types '%s'.",
                    cr.getServiceIdentifier(), cr.getReceiverId(), acceptableTypes);
            log.error(markerFrom(cr), errorStr);
            throw new NextMoveException(errorStr);
        }

        strategy.get().send(cr);
        conversationService.registerStatus(cr.getConversationId(), MessageStatus.of(GenericReceiptStatus.SENDT));
        outRepo.delete(cr);
        try {
            messagePersister.delete(cr.getConversationId());
        } catch (IOException e) {
            log.error("Error deleting files from conversation with id={}", cr.getConversationId(),  e);
        }
    }
}
