package no.difi.meldingsutveksling.cucumber;

import lombok.SneakyThrows;
import lombok.Value;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class MultipartParser {

    @SneakyThrows
    Map<String, FileItemResource> parse(String contentType, byte[] body) {
        //RequestContext requestContext = new SimpleRequestContext(UTF_8, contentType, body);
        RequestContext requestContext = null; // FIXME MultipartParser.java:[27,31] cannot access javax.servlet.http.HttpServletRequest
//        return getFileUpload().parseRequest(requestContext)
//                .stream()
//                .collect(Collectors.toMap(FileItem::getFieldName, FileItemResource::new));
        return null;
    }

    @NotNull
    private FileUpload getFileUpload() {
        //FileUpload fileUpload = new PortletFileUpload();
        FileUpload fileUpload = new FileUpload(); // FIXME MultipartParser.java:[29,31] cannot access javax.servlet.http.HttpServletRequest
        fileUpload.setFileItemFactory(new DiskFileItemFactory());
        fileUpload.setHeaderEncoding(UTF_8.displayName());
        return fileUpload;
    }

    @Value
    private static class SimpleRequestContext implements RequestContext {
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
    }

}
