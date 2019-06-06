package no.difi.meldingsutveksling.cucumber;

import lombok.Data;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.domain.StreamedFile;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Data
class Attachment implements StreamedFile {

    private String fileName;
    private String mimeType;
    private byte[] bytes;

    @SneakyThrows(IOException.class)
    Attachment(InputStream is) {
        this.bytes = IOUtils.toByteArray(is);
    }

    public InputStream getInputStream() {
        return new BufferedInputStream(new ByteArrayInputStream(bytes));
    }
}
