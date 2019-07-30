package no.difi.meldingsutveksling.domain;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@AllArgsConstructor
@NoArgsConstructor
public class NextMoveStreamedFile implements StreamedFile {

    private String fileName;
    private InputStream inputStream;
    private String mimeType;

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }
}
