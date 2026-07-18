package no.difi.meldingsutveksling.altinnv3.dpv;

import org.springframework.http.HttpStatusCode;

public class CorrespondenceApiException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public CorrespondenceApiException() {
        super();
        this.statusCode = null;
    }

    public CorrespondenceApiException(String message) {
        super(message);
        this.statusCode = null;
    }

    public CorrespondenceApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public CorrespondenceApiException(String message, HttpStatusCode statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public boolean isClientError() {
        return statusCode != null && statusCode.is4xxClientError();
    }
}
