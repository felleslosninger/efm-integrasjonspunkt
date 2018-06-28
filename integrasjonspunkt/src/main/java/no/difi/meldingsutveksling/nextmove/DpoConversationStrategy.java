package no.difi.meldingsutveksling.nextmove;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.MessageContextException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers.markerFrom;

@Component
@Slf4j
public class DpoConversationStrategy implements ConversationStrategy {

    private MessageSender messageSender;

    @Autowired
    DpoConversationStrategy(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public void send(ConversationResource cr) throws NextMoveException {
        try {
            messageSender.sendMessage(cr);
        } catch (MessageContextException e) {
            log.error("Send message failed.", e);
            throw new NextMoveException(e);
        }
        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to altinn",
                cr.getConversationId(), cr.getServiceIdentifier()),
                markerFrom(cr));
    }

}
