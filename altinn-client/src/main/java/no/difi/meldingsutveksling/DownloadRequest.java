package no.difi.meldingsutveksling;

public class DownloadRequest {
    final String fileReference;
    final String reciever;

    /**
     * @param fileReference the file reference as returned by available files Altinn service
     * @param reciever the message receiver party number
     */
    public DownloadRequest(String fileReference, String reciever) {
        this.fileReference = fileReference;
        this.reciever = reciever;
    }

    /**
     * @return the file reference as returned by available files Altinn service
     */
    public String getFileReference() {
        return fileReference;
    }

    /**
     * @return the message receiver party number
     */
    public String getReciever() {
        return reciever;
    }
}
