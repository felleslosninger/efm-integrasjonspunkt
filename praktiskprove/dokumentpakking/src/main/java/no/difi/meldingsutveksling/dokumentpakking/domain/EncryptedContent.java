package no.difi.meldingsutveksling.dokumentpakking.domain;

public class EncryptedContent {
	private byte[] key;
	private byte[] content;
	
	public EncryptedContent(byte[] key, byte[] content){
		setContent(content);
		setKey(key);
	}

	public byte[] getKey() {
		return key;
	}

	private void setKey(byte[] key) {
		this.key = key;
	}

	public byte[] getContent() {
		return content;
	}

	private void setContent(byte[] content) {
		this.content = content;
	}
}
