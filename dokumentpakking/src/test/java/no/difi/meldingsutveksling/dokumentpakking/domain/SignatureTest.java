package no.difi.meldingsutveksling.dokumentpakking.domain;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class SignatureTest {
	
	@Test
	public void testGetFileName() throws Exception {
		Signature sig = new Signature(new byte[0]);
		assertThat(sig.getFileName(), is(equalTo("META-INF/signatures.xml")));
	}


	@Test
	public void testGetMimeType() throws Exception {
		Signature sig = new Signature(new byte[0]);
		assertThat(sig.getMimeType(), is(equalTo("application/xml")));
	}


	@Test
	public void testGetBytes() throws Exception {
		byte[] byteArray = new byte[3];
		Signature sig = new Signature(byteArray);
		assertThat(sig.getBytes(), is(equalTo(byteArray)));
		assertThat(sig.getBytes(), is(not(sameInstance(byteArray))));
	}
	
	

}
