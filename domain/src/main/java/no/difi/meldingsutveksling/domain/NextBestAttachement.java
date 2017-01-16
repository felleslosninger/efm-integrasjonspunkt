package no.difi.meldingsutveksling.domain;

public class NextBestAttachement implements ByteArrayFile {
	private ByteArray content;
	private String filename;

	public NextBestAttachement(ByteArray content, String filename) {
		this.content = content;
		this.filename = filename;
	}

	public NextBestAttachement(byte[] content, String filename) {
		this(new ByteArray(content), filename);
	}

	public String getFileName() {
		return this.filename;
	}

	public byte[] getBytes() {
		return content.getByteArray();
	}

	public String getMimeType() {
		return "text/xml";
	}
}
