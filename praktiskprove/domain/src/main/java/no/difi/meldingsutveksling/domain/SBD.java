package no.difi.meldingsutveksling.domain;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA. User: glennbech Date: 10.11.14 Time: 10:14 To
 * change this template use File | Settings | File Templates.
 */
public class SBD {
	private byte content[];

	public SBD(byte[] content) {
		this.content = Arrays.copyOf(content, content.length);
	}

	public SBD() {
	}

	public byte[] getContent() {
		return Arrays.copyOf(content, content.length);
	}
}
