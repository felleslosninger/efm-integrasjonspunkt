package no.difi.meldingsutveksling.domain;

import java.util.Arrays;

public final class ByteArray {
    private final byte[] content;

    ByteArray(byte[] content) {
        this.content = Arrays.copyOf(content, content.length);
    }

    public byte[] getContent() {
        return Arrays.copyOf(content, content.length);
    }
}
