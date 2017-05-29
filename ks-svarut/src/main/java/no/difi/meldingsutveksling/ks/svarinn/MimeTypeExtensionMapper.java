package no.difi.meldingsutveksling.ks.svarinn;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.util.HashMap;

public class MimeTypeExtensionMapper {

    private static final Logger log = LoggerFactory.getLogger(MimeTypeExtensionMapper.class);

    private static final HashMap<String, String> mimeTypeMap = Maps.newHashMap();

    static {
        mimeTypeMap.put("application/msword", "DOC");
        mimeTypeMap.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "DOCX");
        mimeTypeMap.put("application/vnd.ms-excel", "XLS");
        mimeTypeMap.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "XLSX");
        mimeTypeMap.put("application/vnd.openxmlformats-officedocument.spreadsheetml.template", "XLTX");
        mimeTypeMap.put("application/vnd.ms-powerpoint", "PPT");
        mimeTypeMap.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", "PPTX");
        mimeTypeMap.put(MediaType.APPLICATION_PDF_VALUE, "PDF");
    }

    public static String getExtension(String mimeType) {
        if (mimeTypeMap.containsKey(mimeType)) {
            return mimeTypeMap.get(mimeType);
        }

        log.error(String.format("MimeType \'%s\' not in map - defaulting to PDF", mimeType));
        return "PDF";
    }
}
