package no.difi.meldingsutveksling.altinnv3.dpo;

import java.util.UUID;

public class DownloadRequest {
    final UUID fileReference;
    final String reciever;

    /**
     * @param fileReference the file reference as returned by available files Altinn service
     * @param reciever the message receiver party number
     */
    public DownloadRequest(UUID fileReference, String reciever) {
        this.fileReference = fileReference;
        this.reciever = reciever;
    }

    /**
     * @return the file reference as returned by available files Altinn service
     */
    public UUID getFileReference() {
        return fileReference;
    }

    /**
     * @return the message receiver party number
     */
    public String getReciever() {
        return reciever;
    }
}
