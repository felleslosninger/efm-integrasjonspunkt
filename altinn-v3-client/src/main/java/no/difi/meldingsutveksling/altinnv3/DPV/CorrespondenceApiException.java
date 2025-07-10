package no.difi.meldingsutveksling.altinnv3.DPV;

public class CorrespondenceApiException extends RuntimeException {
    public CorrespondenceApiException() {
        super();
    }

    public CorrespondenceApiException(String message) {
        super(message);
    }

    public CorrespondenceApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
