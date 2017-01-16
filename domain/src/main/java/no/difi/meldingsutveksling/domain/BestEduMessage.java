package no.difi.meldingsutveksling.domain;

public class BestEduMessage implements ByteArrayFile {
	private ByteArray content;

	public BestEduMessage(ByteArray content) {
		this.content = content;
	}

	public BestEduMessage(byte[] content) {
		this(new ByteArray(content));
	}

	public String getFileName() {
		return "best_edu.xml";
	}

	public byte[] getBytes() {
		return content.getByteArray();
	}

	public String getMimeType() {
		return "text/xml";
	}
}
