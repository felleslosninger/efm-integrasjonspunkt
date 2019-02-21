package no.difi.meldingsutveksling;

import com.google.common.collect.HashBiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class MimeTypeExtensionMapper {

    private static final Logger log = LoggerFactory.getLogger(MimeTypeExtensionMapper.class);

    private static final HashBiMap<String, String> mimeTypeMap = HashBiMap.create();

    static {
        mimeTypeMap.put("application/pdf", "pdf");
        mimeTypeMap.put("text/html", "html");
        mimeTypeMap.put("text/plain", "txt");
        mimeTypeMap.put("application/msword", "doc");
        mimeTypeMap.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        mimeTypeMap.put("application/vnd.ms-excel", "xls");
        mimeTypeMap.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
        mimeTypeMap.put("application/vnd.openxmlformats-officedocument.spreadsheetml.template", "xltx");
        mimeTypeMap.put("application/vnd.ms-powerpoint", "ppt");
        mimeTypeMap.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");
        mimeTypeMap.put("application/vnd.oasis.opendocument.formula", "odf");
        mimeTypeMap.put("application/vnd.oasis.opendocument.text", "odt");
        mimeTypeMap.put("application/vnd.oasis.opendocument.spreadsheet", "fods");
        mimeTypeMap.put("application/vnd.oasis.opendocument.presentation", "fodp");
        mimeTypeMap.put("application/vnd.oasis.opendocument.graphics", "fodg");
        mimeTypeMap.put("image/gif", "gif");
        mimeTypeMap.put("image/jpeg", "jpeg");
        mimeTypeMap.put("image/png", "png");
        mimeTypeMap.put("application/octet-stream", "zip");
    }

    private MimeTypeExtensionMapper() {
    }

    /**
     * Returns extension based on mimeType.
     *
     * @param mimeType the mime type to map
     * @return the mapped extension, or "pdf" as default if no mapping was found
     */
    public static String getExtension(String mimeType) {
        if (mimeTypeMap.containsKey(mimeType)) {
            return mimeTypeMap.get(mimeType);
        }

        log.error(String.format("MimeType \'%s\' not in map - defaulting to PDF", mimeType));
        return "pdf";
    }

    /**
     * Returns mime type based on extension.
     *
     * @param extension the extension to map
     * @return the mapped mime type, or "application/pdf" as default if no mapping was found
     */
    public static String getMimetype(String extension) {
        String ext = extension.toLowerCase();
        if (mimeTypeMap.containsValue(ext)) {
            return mimeTypeMap.inverse().get(ext);
        }

        return MediaType.APPLICATION_PDF_VALUE;
    }

}
