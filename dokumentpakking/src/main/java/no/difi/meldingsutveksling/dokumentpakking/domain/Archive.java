package no.difi.meldingsutveksling.dokumentpakking.domain;

import java.util.Arrays;

public class Archive {

    private byte[] bytes;

    public Archive(byte[] bytes) {
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    public byte[] getBytes() {
        return  Arrays.copyOf(bytes, bytes.length);
    }
}