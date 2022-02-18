package no.difi.meldingsutveksling.dpi.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class FileExtensionMapper {

    private final Map<String, String> mimeTypeMap = new HashMap<>();

    public FileExtensionMapper() {
        mimeTypeMap.put("pdf", "application/pdf");
        mimeTypeMap.put("html", "text/html");
        mimeTypeMap.put("txt", "text/plain");
        mimeTypeMap.put("xml", "text/xml");
        mimeTypeMap.put("doc", "application/msword");
        mimeTypeMap.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        mimeTypeMap.put("xls", "application/vnd.ms-excel");
        mimeTypeMap.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mimeTypeMap.put("xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template");
        mimeTypeMap.put("ppt", "application/vnd.ms-powerpoint");
        mimeTypeMap.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        mimeTypeMap.put("odf", "application/vnd.oasis.opendocument.formula");
        mimeTypeMap.put("odt", "application/vnd.oasis.opendocument.text");
        mimeTypeMap.put("fods", "application/vnd.oasis.opendocument.spreadsheet");
        mimeTypeMap.put("fodp", "application/vnd.oasis.opendocument.presentation");
        mimeTypeMap.put("fodg", "application/vnd.oasis.opendocument.graphics");
        mimeTypeMap.put("gif", "image/gif");
        mimeTypeMap.put("jpg", "image/jpeg");
        mimeTypeMap.put("jpeg", "image/jpeg");
        mimeTypeMap.put("png", "image/png");
        mimeTypeMap.put("zip", "application/octet-stream");
    }

    public String getMimetype(Resource resource) {
        String name = resource.getFilename();
        if (name == null) {
            return MediaType.APPLICATION_PDF_VALUE;
        }
        String[] parts = name.split("\\.");
        String extension = Stream.of(parts).reduce((p, e) -> e).orElse("pdf");
        return getMimetype(extension);
    }

    public String getMimetype(String extension) {
        String ext = extension.toLowerCase();
        if (mimeTypeMap.containsKey(ext)) {
            return mimeTypeMap.get(ext);
        }

        return MediaType.APPLICATION_PDF_VALUE;
    }
}
