package no.difi.meldingsutveksling.dokumentpakking.domain;

public class Archive {

    private byte[] bytes;

    public Archive(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }
}