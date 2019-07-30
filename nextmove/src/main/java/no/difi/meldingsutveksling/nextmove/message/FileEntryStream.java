package no.difi.meldingsutveksling.nextmove.message;

import lombok.Value;

import java.io.IOException;
import java.io.InputStream;

@Value(staticConstructor = "of")
public class FileEntryStream implements AutoCloseable {

    private final InputStream inputStream;
    private final long size;

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
