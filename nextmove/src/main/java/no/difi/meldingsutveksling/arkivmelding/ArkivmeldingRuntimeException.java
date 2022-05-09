package no.difi.meldingsutveksling.arkivmelding;

public class ArkivmeldingRuntimeException extends RuntimeException {

    public ArkivmeldingRuntimeException(String message) {
        super(message);
    }

    public ArkivmeldingRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
