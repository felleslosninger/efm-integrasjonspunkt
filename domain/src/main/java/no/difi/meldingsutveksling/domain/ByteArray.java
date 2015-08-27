package no.difi.meldingsutveksling.domain;

import java.util.Arrays;

public final class ByteArray {
	private final byte[] byteArray;
	public ByteArray(byte[] byteArray) {
		this.byteArray = Arrays.copyOf(byteArray, byteArray.length);
	}
	
	public byte[] getByteArray(){
		return Arrays.copyOf(byteArray, byteArray.length);
	}
}
