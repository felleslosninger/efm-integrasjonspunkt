package no.difi.meldingsutveksling.ks.svarinn;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.util.HashMap;

public class MimeTypeExtensionMapper {

    private static final Logger log = LoggerFactory.getLogger(MimeTypeExtensionMapper.class);

    private static final HashBiMap<String, String> mimeTypeMap = HashBiMap.create();

    static {
        mimeTypeMap.put("application/msword", "DOC");
        mimeTypeMap.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "DOCX");
        mimeTypeMap.put("application/vnd.ms-excel", "XLS");
        mimeTypeMap.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "XLSX");
        mimeTypeMap.put("application/vnd.openxmlformats-officedocument.spreadsheetml.template", "XLTX");
        mimeTypeMap.put("application/vnd.ms-powerpoint", "PPT");
        mimeTypeMap.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", "PPTX");
        mimeTypeMap.put(MediaType.APPLICATION_PDF_VALUE, "PDF");
        mimeTypeMap.put("text/plain", "TXT");
    }

    private MimeTypeExtensionMapper() {
    }

    /**
     * Returns extension based on mimeType.
     *
     * @param mimeType the mime type to map
     * @return the mapped extension, or "PDF" as default if no mapping was found
     */
    public static String getExtension(String mimeType) {
        if (mimeTypeMap.containsKey(mimeType)) {
            return mimeTypeMap.get(mimeType);
        }

        log.error(String.format("MimeType \'%s\' not in map - defaulting to PDF", mimeType));
        return "PDF";
    }

    /**
     * Returns mime type based on extension.
     *
     * @param extension the extension to map
     * @return the mapped mime type, or "application/pdf" as default if no mapping was found
     */
    public static String getMimetype(String extension) {
        String ext = extension.toUpperCase();
        if (mimeTypeMap.containsValue(ext)) {
            return mimeTypeMap.inverse().get(ext);
        }

        return MediaType.APPLICATION_PDF_VALUE;
    }
}
