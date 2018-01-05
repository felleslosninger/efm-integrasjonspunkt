package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.logging.Audit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers.markerFrom;

@Component
public class DpeConversationStrategy implements ConversationStrategy {

    private static final Logger log = LoggerFactory.getLogger(DpeConversationStrategy.class);

    private IntegrasjonspunktProperties props;
    private NextMoveServiceBus serviceBus;

    @Autowired
    DpeConversationStrategy(IntegrasjonspunktProperties props,
                            NextMoveServiceBus serviceBus) {
        this.props = props;
        this.serviceBus = serviceBus;
    }

    @Override
    public void send(ConversationResource conversationResource) throws NextMoveException {

        if (!props.getNextbest().getServiceBus().isEnable()) {
            String errorString = format("Service Bus disabled, cannot send messages" +
                    " of types %s,%s", ServiceIdentifier.DPE_INNSYN.toString(), ServiceIdentifier.DPE_DATA.toString());
            log.error(markerFrom(conversationResource), errorString);
            throw new NextMoveException(errorString);
        }
        serviceBus.putMessage(conversationResource);
        Audit.info(format("Message [id=%s, serviceIdentifier=%s] sent to service bus",
                conversationResource.getConversationId(), conversationResource.getServiceIdentifier()),
                markerFrom(conversationResource));
    }

}
