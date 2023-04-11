package no.difi.meldingsutveksling.dpi.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class FileExtensionMapper {

    private final Map<String, MimeType> mimeTypeMap = new HashMap<>();

    public FileExtensionMapper() {
        mimeTypeMap.put("pdf", MediaType.APPLICATION_PDF);
        mimeTypeMap.put("html", MediaType.TEXT_HTML);
        mimeTypeMap.put("txt", MediaType.TEXT_PLAIN);
        mimeTypeMap.put("xml", MediaType.TEXT_XML);
        mimeTypeMap.put("doc", MediaType.valueOf("application/msword"));
        mimeTypeMap.put("docx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        mimeTypeMap.put("xls", MediaType.valueOf("application/vnd.ms-excel"));
        mimeTypeMap.put("xlsx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        mimeTypeMap.put("xltx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.template"));
        mimeTypeMap.put("ppt", MediaType.valueOf("application/vnd.ms-powerpoint"));
        mimeTypeMap.put("pptx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        mimeTypeMap.put("odf", MediaType.valueOf("application/vnd.oasis.opendocument.formula"));
        mimeTypeMap.put("odt", MediaType.valueOf("application/vnd.oasis.opendocument.text"));
        mimeTypeMap.put("fods", MediaType.valueOf("application/vnd.oasis.opendocument.spreadsheet"));
        mimeTypeMap.put("fodp", MediaType.valueOf("application/vnd.oasis.opendocument.presentation"));
        mimeTypeMap.put("fodg", MediaType.valueOf("application/vnd.oasis.opendocument.graphics"));
        mimeTypeMap.put("gif", MediaType.IMAGE_GIF);
        mimeTypeMap.put("jpg", MediaType.IMAGE_JPEG);
        mimeTypeMap.put("jpeg", MediaType.IMAGE_JPEG);
        mimeTypeMap.put("png", MediaType.IMAGE_PNG);
        mimeTypeMap.put("zip", MediaType.APPLICATION_OCTET_STREAM);
    }

    public MimeType getMimetype(Resource resource) {
        String name = resource.getFilename();
        if (name == null) {
            return MediaType.APPLICATION_PDF;
        }
        String[] parts = name.split("\\.");
        String extension = Stream.of(parts).reduce((p, e) -> e).orElse("pdf");
        return getMimetype(extension);
    }

    public MimeType getMimetype(String extension) {
        String ext = extension.toLowerCase();
        if (mimeTypeMap.containsKey(ext)) {
            return mimeTypeMap.get(ext);
        }

        return MediaType.APPLICATION_PDF;
    }
}
