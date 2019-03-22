package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.logging.Audit;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@Slf4j
public class DpeConversationStrategy implements ConversationStrategy {

    private final NextMoveServiceBus serviceBus;

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
