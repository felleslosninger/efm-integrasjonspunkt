package no.difi.meldingsutveksling.dpi.client.internal.domain;


import lombok.Value;
import no.difi.meldingsutveksling.dpi.client.domain.AsicEAttachable;
import org.springframework.core.io.Resource;

@Value
public class Manifest implements AsicEAttachable {

    Resource resource;

    @Override
    public String getFilename() {
        return "manifest.xml";
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public String getMimeType() {
        return "application/xml";
    }
}
