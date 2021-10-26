package no.difi.meldingsutveksling.dpi.client.internal.domain;

import lombok.Value;
import no.difi.meldingsutveksling.dpi.client.domain.AsicEAttachable;
import org.springframework.core.io.Resource;

@Value
public class Signature implements AsicEAttachable {

    Resource resource;

    @Override
    public String getFilename() {
        return "META-INF/signatures.xml";
    }

    @Override
    public String getMimeType() {
        return "application/xml";
    }

    @Override
    public Resource getResource() {
        return resource;
    }
}
