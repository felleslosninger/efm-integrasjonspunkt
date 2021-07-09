package no.difi.meldingsutveksling.dpi;

import lombok.Data;
import org.springframework.core.io.Resource;

@Data
public class Document {

    String title;
    String filename;
    Resource resource;
    String mimeType;
    MetadataDocument metadataDocument;
}
