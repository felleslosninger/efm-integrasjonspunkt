package no.difi.meldingsutveksling.dokumentpakking.domain;

public class Manifest implements AsicEAttachable {

	private byte[] manifestXml;

	
	public Manifest(byte[] manifestXml) {
		super();
		this.manifestXml = manifestXml;
	}

	@Override
	public String getFileName() {
		return "manifest.xml";
	}

	@Override
	public byte[] getBytes() {
		return manifestXml;
	}

	@Override
	public String getMimeType() {
		return "application/xml";
	}

}
