package no.difi.meldingsutveksling.ks.svarut;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;

import javax.annotation.PostConstruct;

@RequiredArgsConstructor
public class SvarUtConnectionCheck {

    private final SvarUtService svarUtService;

    @PostConstruct
    public void checkTheConnection() {
        try {
            svarUtService.getMessageReceipt("1");
        } catch (Exception e) {
            throw new NextMoveRuntimeException("Couldn't retrieve forsendelse typer from SvarUt");
        }
    }
}
