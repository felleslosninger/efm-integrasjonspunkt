package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Data;
import org.springframework.core.io.Resource;

@Data
public class Document implements AsicEAttachable {

    String title;
    String filename;
    Resource resource;
    String mimeType;
    MetadataDocument metadataDocument;
}
