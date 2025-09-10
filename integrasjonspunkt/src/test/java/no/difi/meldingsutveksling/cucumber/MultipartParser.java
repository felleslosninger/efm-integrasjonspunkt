package no.difi.meldingsutveksling.cucumber;

import lombok.SneakyThrows;
import lombok.Value;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.apache.tomcat.util.http.fileupload.RequestContext;
import org.apache.tomcat.util.http.fileupload.UploadContext;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class MultipartParser {

    @SneakyThrows
    Map<String, FileItemResource> parse(String contentType, byte[] body) {
        RequestContext requestContext = new SimpleRequestContext(UTF_8, contentType, body);
        return getFileUpload().parseRequest(requestContext)
                .stream()
                .collect(Collectors.toMap(FileItem::getFieldName, FileItemResource::new));
    }

    @NotNull
    private FileUpload getFileUpload() {
        FileUpload fileUpload = new FileUpload();
        fileUpload.setFileItemFactory(new DiskFileItemFactory());
        fileUpload.setHeaderEncoding(UTF_8.displayName());
        return fileUpload;
    }

    @Value
    private static class SimpleRequestContext implements UploadContext {
        Charset charset;
        String contentType;
        byte[] content;

        public String getCharacterEncoding() {
            return charset.displayName();
        }

        public String getContentType() {
            return contentType;
        }

        public int getContentLength() {
            return content.length;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public long contentLength() {
            return getContentLength();
        }
    }

}
