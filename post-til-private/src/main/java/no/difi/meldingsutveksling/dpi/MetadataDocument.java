package no.difi.meldingsutveksling.dpi;

import lombok.Data;
import org.springframework.core.io.Resource;

@Data
public class MetadataDocument {

    String filename;
    Resource resource;
    String mimeType;
}
