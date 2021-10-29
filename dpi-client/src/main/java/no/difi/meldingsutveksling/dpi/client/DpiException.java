package no.difi.meldingsutveksling.dpi.client;

import lombok.Getter;

public class DpiException extends RuntimeException {

    @Getter
    private final Blame blame;

    public DpiException(String message, Blame blame) {
        super(message);
        this.blame = blame;
    }

    public DpiException(String message, Throwable cause, Blame blame) {
        super(message, cause);
        this.blame = blame;
    }
}
