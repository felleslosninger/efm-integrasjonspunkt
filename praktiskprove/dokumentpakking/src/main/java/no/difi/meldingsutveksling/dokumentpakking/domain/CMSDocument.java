package no.difi.meldingsutveksling.dokumentpakking.domain;

public class CMSDocument {
    private final byte[] bytes;

    public CMSDocument(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
