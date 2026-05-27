package no.difi.meldingsutveksling.dph.client;

import lombok.Getter;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;

@Getter
public class DphException extends RuntimeException {

    private final String errorCode;
    private final Integer statusCode;

    public DphException(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public DphException(String errorCode, String message, Integer statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public DphException(FeilmeldingForApplikasjonskvittering feilmelding) {
        super(feilmelding.getNavn());
        this.errorCode = feilmelding.getVerdi();
        this.statusCode = null;
    }
}
