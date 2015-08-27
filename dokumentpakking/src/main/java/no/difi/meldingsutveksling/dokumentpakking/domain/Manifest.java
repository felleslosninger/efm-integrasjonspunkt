package no.difi.meldingsutveksling.dokumentpakking.domain;

import java.util.Arrays;

import no.difi.meldingsutveksling.domain.ByteArrayFile;

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
		return "application/xml";
	}

}
