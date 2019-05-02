package no.difi.meldingsutveksling.cucumber;

import lombok.Data;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.domain.StreamedFile;
import no.difi.meldingsutveksling.pipes.Pipe;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.UUID;

import static no.difi.meldingsutveksling.pipes.PipeOperations.close;
import static no.difi.meldingsutveksling.pipes.PipeOperations.copy;

@Data
class Attachment implements StreamedFile {

    private String fileName;
    private String mimeType;
    private final File file;

    @SneakyThrows(IOException.class)
    Attachment() {
        this.file = File.createTempFile(UUID.randomUUID().toString(), null);
    }

    @SneakyThrows(IOException.class)
    Attachment(InputStream is) {
        this();
        try (OutputStream os = getOutputStream()) {
            IOUtils.copy(is, os);
            os.flush();
        }
    }

    @SneakyThrows(FileNotFoundException.class)
    public InputStream getInputStream() {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        return Pipe.of("reading file", copy(inputStream).andThen(close(inputStream))).outlet();
    }

    @SneakyThrows(FileNotFoundException.class)
    private OutputStream getOutputStream() {
        return new BufferedOutputStream(new FileOutputStream(file));
    }
}
