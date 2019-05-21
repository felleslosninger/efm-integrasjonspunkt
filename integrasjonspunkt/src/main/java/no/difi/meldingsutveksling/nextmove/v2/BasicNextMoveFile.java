package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor(staticName = "of")
public class BasicNextMoveFile implements MultipartFile  {

    private final String title;
    private final String filename;
    private final String contentType;
    private final byte[] content;

    /**
     * Return the name of the parameter in the multipart form.
     *
     * @return the name of the parameter (never {@code null} or empty)
     */
    @Override
    public String getName() {
        return title;
    }

    @Override
    public String getOriginalFilename() {
        return filename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        throw new UnsupportedOperationException();
    }
}
