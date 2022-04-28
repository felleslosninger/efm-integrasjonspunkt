package no.difi.meldingsutveksling.ks.svarinn;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;

import javax.annotation.PostConstruct;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@RequiredArgsConstructor
public class SvarInnConnectionCheck {

    private final SvarInnClient svarInnClient;
    private final IntegrasjonspunktProperties properties;

    @PostConstruct
    public void checkTheConnection() {
        try {
            if (!isNullOrEmpty(properties.getFiks().getInn().getUsername())) {
                List<Forsendelse> forsendelses = svarInnClient.checkForNewMessages(properties.getFiks().getInn().getOrgnr());
                if (forsendelses == null) {
                    throw new NextMoveRuntimeException("Couldn't check for new messages from SvarInn.");
                }
            }

            properties.getFiks().getInn().getPaaVegneAv().keySet().forEach(orgnr -> {
                List<Forsendelse> forsendelseList = svarInnClient.checkForNewMessages(orgnr);
                if (forsendelseList == null) {
                    throw new NextMoveRuntimeException(String.format("Couldn't check for new messages from SvarInn for orgnr %s.", orgnr));
                }
            });
        } catch (Exception e) {
            throw new NextMoveRuntimeException("Couldn't check for new messages from SvarInn.", e);
        }
    }
}
