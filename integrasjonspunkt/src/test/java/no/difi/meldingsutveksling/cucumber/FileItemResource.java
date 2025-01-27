package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.springframework.core.io.AbstractResource;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor
public class FileItemResource extends AbstractResource {
    private final FileItem fileItem;

    @NonNull
    @Override
    public String getDescription() {
        return "FileItemResource";
    }

    @NonNull
    @Override
    public InputStream getInputStream() throws IOException {
        return fileItem.getInputStream();
    }

    public String getContentType() {
        return fileItem.getContentType();
    }

    public String getName() {
        return fileItem.getName();
    }

    public byte[] getByteArray() {
        return fileItem.get();
    }
}
