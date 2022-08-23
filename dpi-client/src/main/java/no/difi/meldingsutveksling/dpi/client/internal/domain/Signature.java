package no.difi.meldingsutveksling.dpi.client.internal.domain;

import lombok.Value;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

@Value
public class Signature implements AsicEAttachable {

    Resource resource;

    @Override
    public String getFilename() {
        return "META-INF/signatures.xml";
    }

    @Override
    public MimeType getMimeType() {
        return MediaType.APPLICATION_XML;
    }

    @Override
    public Resource getResource() {
        return resource;
    }
}
