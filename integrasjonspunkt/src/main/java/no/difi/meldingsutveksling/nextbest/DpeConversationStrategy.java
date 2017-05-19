package no.difi.meldingsutveksling.nextbest;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.nextbest.logging.ConversationResourceMarkers.markerFrom;

@Component
public class DpeConversationStrategy implements ConversationStrategy {

    private static final Logger log = LoggerFactory.getLogger(DpeConversationStrategy.class);

    private IntegrasjonspunktProperties props;
    private NextBestServiceBus serviceBus;

    @Autowired
    DpeConversationStrategy(IntegrasjonspunktProperties props,
                            NextBestServiceBus serviceBus) {
        this.props = props;
        this.serviceBus = serviceBus;
    }

    @Override
    public ResponseEntity send(ConversationResource conversationResource) {

        if (!props.getNextbest().getServiceBus().isEnable()) {
            String responseStr = String.format("Service Bus disabled, cannot send messages" +
                    " of types %s,%s", ServiceIdentifier.DPE_INNSYN.toString(), ServiceIdentifier.DPE_DATA.toString());
            log.error(markerFrom(conversationResource), responseStr);
            return ResponseEntity.badRequest().body(responseStr);
        }
        try {
            serviceBus.putMessage(conversationResource);
        } catch (NextBestException e) {
            log.error("Send message failed.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during sending. Check logs");
        }
        log.info(markerFrom(conversationResource), "Message sent to service bus");

        return ResponseEntity.ok().build();
    }

}
