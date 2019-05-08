package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingException;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.logging.Audit;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;

@Component
@Slf4j
@RequiredArgsConstructor
public class DpfConversationStrategy implements ConversationStrategy {

    private final SvarUtService svarUtService;

    @Override
    public void send(ConversationResource conversationResource) throws NextMoveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(NextMoveMessage message) throws NextMoveException {
        try {
            svarUtService.send(message);
        } catch (ArkivmeldingException e) {
            throw new NextMoveException(String.format("Error sending message with conversationId=%s to SvarUt", message.getConversationId()), e);
        }

        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to SvarUt",
                message.getConversationId(), message.getServiceIdentifier()),
                markerFrom(message));
    }
}
