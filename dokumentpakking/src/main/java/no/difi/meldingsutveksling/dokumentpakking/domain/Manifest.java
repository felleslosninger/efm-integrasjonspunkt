package no.difi.meldingsutveksling.dokumentpakking.domain;

import no.difi.meldingsutveksling.domain.ByteArrayFile;

import java.util.Arrays;

public class Manifest implements ByteArrayFile {

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
		return "text/xml";
	}

}
