package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.exceptions.MissingHttpHeaderException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class NextMoveUploadedFile implements MultipartFile {

    private final String contentType;
    private final String title;
    private final HttpServletRequest request;
    private final String originalFilename;

    NextMoveUploadedFile(String contentType, String contentDispositionString, String title, HttpServletRequest request) {
        this.contentType = contentType;
        this.title = title;
        this.request = request;

        ContentDisposition contentDisposition = Optional.ofNullable(contentDispositionString)
                .map(ContentDisposition::parse)
                .orElseThrow(() -> new MissingHttpHeaderException(HttpHeaders.CONTENT_DISPOSITION));

        this.originalFilename = contentDisposition.getFilename();
    }


    @Override
    public String getName() {
        return title;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return request.getContentLengthLong() == 0;
    }

    @Override
    public long getSize() {
        return request.getContentLengthLong();
    }

    @Override
    public byte[] getBytes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return request.getInputStream();
    }

    @Override
    public void transferTo(File dest) {
        throw new UnsupportedOperationException();
    }
}
