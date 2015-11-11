package no.difi.virksert.client;

public class VirksertClientException extends Exception {
    public VirksertClientException(String message) {
        super(message);
    }

    public VirksertClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
