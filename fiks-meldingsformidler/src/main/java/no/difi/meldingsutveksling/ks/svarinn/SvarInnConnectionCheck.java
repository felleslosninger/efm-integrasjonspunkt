package no.difi.meldingsutveksling.ks.svarinn;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;

import javax.annotation.PostConstruct;
import java.util.List;

@RequiredArgsConstructor
public class SvarInnConnectionCheck {

    private final SvarInnClient svarInnClient;

    @PostConstruct
    public void checkTheConnection() {
        try {
            List<Forsendelse> forsendelses = svarInnClient.checkForNewMessages();
            if (forsendelses == null || forsendelses.isEmpty()) {
                throw new NextMoveRuntimeException("Couldn't check for new messages from SvarInn.");
            }
        } catch (Exception e) {
            throw new NextMoveRuntimeException("Couldn't check for new messages from SvarInn.", e);
        }
    }
}
