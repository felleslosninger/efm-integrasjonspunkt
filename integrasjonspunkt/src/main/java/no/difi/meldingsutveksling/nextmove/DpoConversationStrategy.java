package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.MessageContextException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@Slf4j
public class DpoConversationStrategy implements ConversationStrategy {

    private final MessageSender messageSender;

    @Override
    public void send(ConversationResource cr) throws NextMoveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(NextMoveOutMessage message) throws NextMoveException {
        try {
            messageSender.sendMessage(message);
        } catch (MessageContextException e) {
            throw new NextMoveException(String.format("Error sending message with conversationId=%s to Altinn", message.getConversationId()), e);
        }

        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to altinn",
                message.getConversationId(), message.getServiceIdentifier()),
                markerFrom(message));
    }

}
