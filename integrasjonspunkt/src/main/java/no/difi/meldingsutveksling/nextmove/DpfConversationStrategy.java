package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.logging.Audit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class DpfConversationStrategy implements ConversationStrategy {

    private final SvarUtService svarUtService;

    @Override
    public void send(ConversationResource conversationResource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(NextMoveMessage message) throws NextMoveException {
        svarUtService.send(message);

        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to SvarUt",
                message.getConversationId(), message.getServiceIdentifier()),
                markerFrom(message));
    }
}
