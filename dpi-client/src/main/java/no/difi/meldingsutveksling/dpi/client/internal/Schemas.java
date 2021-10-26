package no.difi.meldingsutveksling.dpi.client.internal;

import org.springframework.core.io.ClassPathResource;

public class Schemas {

    public static final ClassPathResource SDP_MANIFEST_SCHEMA = new ClassPathResource("xsd/sdp-manifest.xsd");
    public static final ClassPathResource ASICE_SCHEMA = new ClassPathResource("xsd/asic-e/ts_102918v010201.xsd");

    private Schemas() {
    }
}
