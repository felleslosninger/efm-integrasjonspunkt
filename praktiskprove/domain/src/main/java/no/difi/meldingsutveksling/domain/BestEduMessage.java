package no.difi.meldingsutveksling.domain;

/**
 * Created with IntelliJ IDEA. User: glennbech Date: 10.11.14 Time: 10:36 To
 * change this template use File | Settings | File Templates.
 */
public class BestEduMessage implements ByteArrayFile {
	private ByteArray content;

	public BestEduMessage(ByteArray content) {
		this.content = content;
	}

	public BestEduMessage(byte[] content) {
		this(new ByteArray(content));
	}

	public String getFileName() {
		return "edu_best.xml";
	}

	public byte[] getBytes() {
		return content.getByteArray();
	}

	public String getMimeType() {
		return "text/xml";
	}
}
