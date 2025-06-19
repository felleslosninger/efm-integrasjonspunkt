package no.difi.meldingsutveksling.altinnv3.DPO;

public class BrokerApiException extends RuntimeException {

    public BrokerApiException() {
        super();
    }

    public BrokerApiException(String message) {
        super(message);
    }

    public BrokerApiException(String message, Throwable cause) {
        super(message, cause);
    }

}
