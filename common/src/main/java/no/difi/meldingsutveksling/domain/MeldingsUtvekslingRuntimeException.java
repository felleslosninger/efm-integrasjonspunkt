package no.difi.meldingsutveksling.domain;

/**
 * Created by kubkaray on 16.12.2014.
 */
public class MeldingsUtvekslingRuntimeException extends RuntimeException {

    public MeldingsUtvekslingRuntimeException(Throwable e) {
        super(e);
    }

    public MeldingsUtvekslingRuntimeException(String message) {
        super(message);
    }

    public MeldingsUtvekslingRuntimeException(String s, Exception e) {
        super(s, e);
    }

    public MeldingsUtvekslingRuntimeException() {
        super();
    }
}
