package no.difi.meldingsutveksling.ks.svarut;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;

import jakarta.annotation.PostConstruct;

import static com.google.common.base.Strings.isNullOrEmpty;

@RequiredArgsConstructor
public class SvarUtConnectionCheck {

    private final SvarUtService svarUtService;
    private final IntegrasjonspunktProperties properties;

    @PostConstruct
    public void checkTheConnection() {
        try {
            if (!isNullOrEmpty(properties.getFiks().getUt().getUsername())) {
                svarUtService.retreiveForsendelseTyper(properties.getOrg().getNumber());
            }

            properties.getFiks().getUt().getPaaVegneAv().keySet().forEach(svarUtService::retreiveForsendelseTyper);
        } catch (Exception e) {
            String message = "SvarUt connection check failed. Couldn't retrieve forsendelse typer from SvarUt.";
            String errorMessage = e.getMessage();
            if(errorMessage != null && errorMessage.contains("401")) {
                message += " Unauthorized (401) when calling SvarUt, this can be caused by wrong username or password, please ensure that difi.move.fiks.ut.username and difi.move.fiks.ut.password in properties are correct.";
            }

            throw new NextMoveRuntimeException(message, e);
        }
    }
}
