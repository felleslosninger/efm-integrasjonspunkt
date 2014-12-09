package no.difi.meldingsutveksling.knutepunkrecieve;

import org.junit.Ignore;
import org.junit.Test;

public class ReceiveTest {


	@Test @Ignore
	public void testSendEduMeldig() throws Exception {
		new Receive("http://localhost:9090/knutepunkt/receive").callReceive(null);
	}

}
