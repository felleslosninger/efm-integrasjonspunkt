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
        this(errorCode, message, statusCode, null);
    }

    public DphException(String errorCode, String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    public DphException(FeilmeldingForApplikasjonskvittering feilmelding) {
        super(feilmelding.getNavn());
        this.errorCode = feilmelding.getVerdi();
        this.statusCode = null;
    }

    /**
     * Whether the adapter answered that the resource does not exist (any more), e.g. a business document that has
     * already been downloaded.
     */
    public boolean isNotFound() {
        return statusCode != null && (statusCode == 404 || statusCode == 410);
    }
}
