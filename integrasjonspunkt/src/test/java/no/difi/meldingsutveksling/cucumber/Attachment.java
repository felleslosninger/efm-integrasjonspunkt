package no.difi.meldingsutveksling.cucumber;

import lombok.Data;
import no.difi.meldingsutveksling.domain.StreamedFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Data
class Attachment implements StreamedFile {

    private String fileName;
    private String mimeType;
    private byte[] bytes;

    Attachment(byte[] bytes) {
        this.bytes = bytes;
    }

    public InputStream getInputStream() {
        return new BufferedInputStream(new ByteArrayInputStream(bytes));
    }
}
