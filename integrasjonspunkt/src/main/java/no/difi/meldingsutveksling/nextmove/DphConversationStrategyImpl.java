package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import no.difi.meldingsutveksling.api.ConversationStrategy;
import org.springframework.stereotype.Component;

@Component
public class DphConversationStrategyImpl implements ConversationStrategy {
    @Override
    @Timed
    public void send(NextMoveOutMessage message) throws NextMoveException {

    }
}
