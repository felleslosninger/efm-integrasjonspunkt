package no.difi.meldingsutveksling.noark;

public class UnknownArchiveSystemException extends RuntimeException {
    public UnknownArchiveSystemException(String archiveSystemName) {
        super("Unknown archive system " + archiveSystemName);
    }
}
