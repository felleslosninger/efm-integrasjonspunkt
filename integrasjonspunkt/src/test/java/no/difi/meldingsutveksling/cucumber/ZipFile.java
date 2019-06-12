package no.difi.meldingsutveksling.cucumber;

import lombok.Value;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Value
class ZipFile {

    private final String fileName;
    private final byte[] payload;

    public InputStream getInputStream() {
        return new ByteArrayInputStream(payload);
    }
}
