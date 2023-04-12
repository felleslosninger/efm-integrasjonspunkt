package no.difi.meldingsutveksling.ks.svarut;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;

import javax.annotation.PostConstruct;

import static com.google.common.base.Strings.isNullOrEmpty;

@RequiredArgsConstructor
public class SvarUtConnectionCheck {

    private final SvarUtService svarUtService;
    private final IntegrasjonspunktProperties properties;

    @PostConstruct
    public void checkTheConnection() {
        try {
            if (!isNullOrEmpty(properties.getFiks().getUt().getUsername())) {
                svarUtService.retreiveForsendelseTyper(properties.getOrg().getIdentifier().getIdentifier());
            }

            properties.getFiks().getUt().getPaaVegneAv().keySet().forEach(svarUtService::retreiveForsendelseTyper);
        } catch (Exception e) {
            throw new NextMoveRuntimeException("Couldn't retrieve forsendelse typer from SvarUt", e);
        }
    }
}
