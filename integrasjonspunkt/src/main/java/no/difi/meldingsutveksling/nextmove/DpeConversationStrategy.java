package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.logging.Audit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

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
        throw new UnsupportedOperationException("ConversationResource no longer in use");
    }

    @Override
    public void send(NextMoveMessage message) throws NextMoveException {
        serviceBus.putMessage(message);
        Audit.info(format("Message [id=%s, serviceIdentifier=%s] sent to service bus",
                message.getConversationId(), message.getServiceIdentifier()),
                markerFrom(message));
    }

}
