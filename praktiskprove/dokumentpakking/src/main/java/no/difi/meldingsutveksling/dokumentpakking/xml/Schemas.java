package no.difi.meldingsutveksling.dokumentpakking.xml;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public final class Schemas {

	private Schemas() {
	}

	public static final ClassPathResource SBDH_SCHEMA = new ClassPathResource("SBDH20040506-02/StandardBusinessDocumentHeader.xsd");
	public static final ClassPathResource SDP_SCHEMA = new ClassPathResource("sdp.xsd");
	public static final ClassPathResource SDP_MANIFEST_SCHEMA = new ClassPathResource("sdp-manifest.xsd");
	public static final ClassPathResource EBMS_SCHEMA = new ClassPathResource("ebxml/ebms-header-3_0-200704.xsd");
	public static final ClassPathResource XMLDSIG_SCHEMA = new ClassPathResource("w3/xmldsig-core-schema.xsd");
	public static final ClassPathResource XADES_SCHEMA = new ClassPathResource("etsi/XAdES.xsd");
	public static final ClassPathResource ASICE_SCHEMA = new ClassPathResource("asic-e/ts_102918v010201.xsd");

	public static Resource[] allSchemaResources() {
		return new Resource[] { SDP_SCHEMA, SDP_MANIFEST_SCHEMA, SBDH_SCHEMA, EBMS_SCHEMA, XMLDSIG_SCHEMA, XADES_SCHEMA, ASICE_SCHEMA };
	}

	public static Resource[] sbdResources() {
		return new Resource[] { SBDH_SCHEMA, SDP_SCHEMA, XMLDSIG_SCHEMA };
	}
}