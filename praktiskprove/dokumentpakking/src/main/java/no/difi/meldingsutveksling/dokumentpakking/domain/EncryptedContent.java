package no.difi.meldingsutveksling.dokumentpakking.domain;

import java.util.Arrays;

public class EncryptedContent {
	private byte[] key;
	private byte[] content;
	
	public EncryptedContent(byte[] key, byte[] content){
		setContent(Arrays.copyOf(content, content.length));
		setKey(Arrays.copyOf(key, key.length));
	}

	public byte[] getKey() {
		return Arrays.copyOf(key, key.length);
	}

	private void setKey(byte[] key) {
		this.key = Arrays.copyOf(key, key.length);
	}

	public byte[] getContent() {
		return Arrays.copyOf(content, content.length);
	}

	private void setContent(byte[] content) {
		this.content = Arrays.copyOf(content, content.length);
	}
}
