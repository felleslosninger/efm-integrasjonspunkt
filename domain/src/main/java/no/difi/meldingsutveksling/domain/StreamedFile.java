package no.difi.meldingsutveksling.domain;

import java.io.InputStream;

public interface StreamedFile {
    String getFileName();
    InputStream getInputStream();
    String getMimeType();
}
