package no.difi.meldingsutveksling.dpi.client;

import no.difi.asic.AsicReader;
import no.difi.asic.AsicReaderFactory;

import java.io.IOException;
import java.io.InputStream;

public class AsicParser {

    void parse(InputStream inputStream, Listener listener) {
        try (AsicReader asicReader = AsicReaderFactory.newFactory().open(inputStream)) {

            String filename;

            while ((filename = asicReader.getNextFile()) != null) {
                try (InputStream is = asicReader.inputStream()) {
                    listener.onFile(filename, is);
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Couldn't parse ASICe", e);
        }
    }

    public interface Listener {

        void onFile(String filename, InputStream inputStream);
    }

}
