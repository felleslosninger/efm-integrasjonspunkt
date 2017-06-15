package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.noarkexchange.MessageContextException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers.markerFrom;

@Component
public class DpoConversationStrategy implements ConversationStrategy {

    private static final Logger log = LoggerFactory.getLogger(DpoConversationStrategy.class);

    private ServiceRegistryLookup sr;
    private MessageSender messageSender;

    @Autowired
    DpoConversationStrategy(ServiceRegistryLookup sr,
                            MessageSender messageSender) {
        this.sr = sr;
        this.messageSender = messageSender;
    }

    @Override
    public ResponseEntity send(ConversationResource conversationResource) {
        ServiceRecord serviceRecord = sr.getServiceRecord(conversationResource.getReceiverId());
        if (!serviceRecord.getServiceIdentifier().equals(ServiceIdentifier.DPO)) {
            String errorStr = String.format("Cannot send DPO message - receiver has ServiceIdentifier \"%s\"",
                    serviceRecord.getServiceIdentifier());
            log.error(markerFrom(conversationResource), errorStr);
            return ResponseEntity.badRequest().body(errorStr);
        }
        try {
            messageSender.sendMessage(conversationResource);
        } catch (MessageContextException e) {
            log.error("Send message failed.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during sending. Check logs");
        }
        log.info(markerFrom(conversationResource), "Message sent to altinn");

        return ResponseEntity.ok().build();
    }

}
