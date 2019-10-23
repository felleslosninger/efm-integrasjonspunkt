package no.difi.meldingsutveksling.pipes;

class PromiseRuntimeException extends RuntimeException {

    PromiseRuntimeException(String message) {
        super(message);
    }

    PromiseRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
