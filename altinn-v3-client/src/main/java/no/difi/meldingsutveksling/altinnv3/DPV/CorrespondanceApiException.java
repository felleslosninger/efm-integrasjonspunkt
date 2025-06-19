package no.difi.meldingsutveksling.altinnv3.DPV;

public class CorrespondanceApiException extends RuntimeException {
    public CorrespondanceApiException() {
        super();
    }

    public CorrespondanceApiException(String message) {
        super(message);
    }

    public CorrespondanceApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
