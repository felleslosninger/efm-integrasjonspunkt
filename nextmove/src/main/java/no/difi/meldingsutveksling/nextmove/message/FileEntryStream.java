package no.difi.meldingsutveksling.nextmove.message;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;

@Data
@AllArgsConstructor(staticName = "of")
public class FileEntryStream implements AutoCloseable {

    private InputStream inputStream;
    private long size;

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
