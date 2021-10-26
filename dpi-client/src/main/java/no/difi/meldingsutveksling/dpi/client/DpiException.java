package no.difi.meldingsutveksling.dpi.client;

import lombok.Getter;

public class DpiException extends RuntimeException {

    @Getter
    private final no.difi.meldingsutveksling.dpi.client.Blame blame;

    public DpiException(String message, no.difi.meldingsutveksling.dpi.client.Blame blame) {
        super(message);
        this.blame = blame;
    }

    public DpiException(String message, Throwable cause, no.difi.meldingsutveksling.dpi.client.Blame blame) {
        super(message, cause);
        this.blame = blame;
    }
}
