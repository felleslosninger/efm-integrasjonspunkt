package no.difi.meldingsutveksling.dph.client;

import lombok.Getter;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;

@Getter
public class DphException extends RuntimeException {

    private final String errorCode;

    public DphException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DphException(FeilmeldingForApplikasjonskvittering feilmelding) {
        super(feilmelding.getNavn());
        this.errorCode = feilmelding.getVerdi();
    }
}
