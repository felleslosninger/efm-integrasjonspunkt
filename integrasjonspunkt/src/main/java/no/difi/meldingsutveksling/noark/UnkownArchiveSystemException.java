package no.difi.meldingsutveksling.noark;

public class UnkownArchiveSystemException extends RuntimeException {
    public UnkownArchiveSystemException(String archiveSystemName) {
        super("Unknown archive system " + archiveSystemName);
    }
}
