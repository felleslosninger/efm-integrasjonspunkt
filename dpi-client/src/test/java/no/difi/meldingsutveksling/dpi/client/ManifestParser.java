package no.difi.meldingsutveksling.dpi.client;

import lombok.SneakyThrows;
import no.difi.meldingsutveksling.dpi.client.internal.Schemas;
import no.difi.meldingsutveksling.dpi.client.sdp.SDPManifest;
import org.springframework.core.io.Resource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;

public class ManifestParser {

    private final Jaxb2Marshaller marshaller;

    @SneakyThrows
    public ManifestParser() {
        this.marshaller = createJaxb2Marshaller();
    }

    private Jaxb2Marshaller createJaxb2Marshaller() throws java.lang.Exception {
        Jaxb2Marshaller m = new Jaxb2Marshaller();
        m.setClassesToBeBound(SDPManifest.class);
        m.setSchema(Schemas.SDP_MANIFEST_SCHEMA);
        m.afterPropertiesSet();
        return m;
    }

    public SDPManifest parse(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return (SDPManifest) marshaller.unmarshal(new StreamSource(inputStream));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse SDPManifest!", e);
        }
    }

}
