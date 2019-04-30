package no.difi.meldingsutveksling.cucumber;

import lombok.Getter;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.pipes.Pipe;

import java.io.*;
import java.util.UUID;

import static no.difi.meldingsutveksling.pipes.PipeOperations.close;
import static no.difi.meldingsutveksling.pipes.PipeOperations.copy;

class ZipFile {

    @Getter
    private final String fileName;
    private final File file;

    @SneakyThrows(IOException.class)
    ZipFile(String fileName) {
        this.fileName = fileName;
        this.file = File.createTempFile(UUID.randomUUID().toString(), null);
    }

    @SneakyThrows(FileNotFoundException.class)
    InputStream getInputStream() {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        return Pipe.of("reading file", copy(inputStream).andThen(close(inputStream))).outlet();
    }

    @SneakyThrows(FileNotFoundException.class)
    OutputStream getOutputStream() {
        return new BufferedOutputStream(new FileOutputStream(file));
    }
}
