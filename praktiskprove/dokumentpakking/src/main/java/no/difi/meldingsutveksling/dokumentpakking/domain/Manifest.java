package no.difi.meldingsutveksling.dokumentpakking.domain;

import java.util.Arrays;

public class Manifest implements AsicEAttachable {

	private byte[] manifestXml;

	
	public Manifest(byte[] manifestXml) {
		super();
		this.manifestXml = Arrays.copyOf(manifestXml, manifestXml.length);
	}

	@Override
	public String getFileName() {
		return "manifest.xml";
	}

	@Override
	public byte[] getBytes() {
		return Arrays.copyOf(manifestXml, manifestXml.length);
	}

	@Override
	public String getMimeType() {
		return "application/xml";
	}

}
