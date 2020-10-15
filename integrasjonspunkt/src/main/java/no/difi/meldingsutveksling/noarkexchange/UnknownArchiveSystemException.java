package no.difi.meldingsutveksling.noarkexchange;

public class UnknownArchiveSystemException extends RuntimeException {
    public UnknownArchiveSystemException(String archiveSystemName) {
        super("Unknown archive system " + archiveSystemName);
    }
}
