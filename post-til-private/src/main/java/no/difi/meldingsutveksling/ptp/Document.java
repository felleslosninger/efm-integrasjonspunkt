package no.difi.meldingsutveksling.ptp;

import java.io.InputStream;

public class Document {
    private InputStream contents;
    private String mimeType;
    private String fileName;
    private String title;
    private InputStream inputStream;

    public InputStream getInputStream() {
        return inputStream;
    }
}
