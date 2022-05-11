package no.difi.meldingsutveksling.dpi.client.internal;

import org.springframework.core.io.ClassPathResource;

public class Schemas {

    public static final ClassPathResource SDP_MANIFEST_SCHEMA = new ClassPathResource("xsd/sdp-manifest.xsd");

    private Schemas() {
    }
}
