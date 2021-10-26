package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Data;
import org.springframework.core.io.Resource;

@Data
public class MetadataDocument implements AsicEAttachable {

    String filename;
    Resource resource;
    String mimeType;
}
