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

    private final HttpServletRequest request;
    private final String originalFilename;

    NextMoveUploadedFile(HttpServletRequest request) {
        this.request = request;

        ContentDisposition contentDisposition = Optional.ofNullable(request.getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .map(ContentDisposition::parse)
                .orElseThrow(() -> new MissingHttpHeaderException(HttpHeaders.CONTENT_DISPOSITION));

        this.originalFilename = contentDisposition.getFilename();
    }

    @Override
    public String getName() {
        return request.getParameter("title");
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return request.getContentType();
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
