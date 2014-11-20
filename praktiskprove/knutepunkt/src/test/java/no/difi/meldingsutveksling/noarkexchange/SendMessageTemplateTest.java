package no.difi.meldingsutveksling.noarkexchange;

import java.io.IOException;
import java.security.PrivateKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import no.difi.meldingsutveksling.domain.SBD;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;

import org.junit.Test;

public class SendMessageTemplateTest {

	SendMessageTemplate subject = new SendMessageTemplate() {

		@Override
		void sendSBD(SBD sbd) throws IOException {
			// Do nothing
		}
	};



	@Test
	public void testVerifySender() throws Exception {
		AddressType adresseType = new AddressType();
		adresseType.setOrgnr("960885406");
		KnutepunktContext context = new KnutepunktContext();
		subject.verifySender(adresseType, context);
		assertThat(context.getAvsender(), is(notNullValue()));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testVerifySenderInvalid() throws Exception {
		AddressType adresseType = new AddressType();
		adresseType.setOrgnr("12345678");
		KnutepunktContext context = new KnutepunktContext();
		subject.verifySender(adresseType, context);
		assertThat(context.getAvsender(), is(notNullValue()));
	}

	@Test
	public void testVerifyRecipient() throws Exception {
		AddressType adresseType = new AddressType();
		adresseType.setOrgnr("960885406");
		KnutepunktContext context = new KnutepunktContext();
		subject.verifyRecipient(adresseType, context);
		assertThat(context.getMottaker(), is(notNullValue()));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testVerifyRecipientInvalid() throws Exception {
		AddressType adresseType = new AddressType();
		adresseType.setOrgnr("12345678");
		KnutepunktContext context = new KnutepunktContext();
		subject.verifyRecipient(adresseType, context);
		assertThat(context.getMottaker(), is(notNullValue()));
	}


	@Test
	public void testFindPrivateKey() throws Exception {
		PrivateKey pk = subject.findPrivateKey();
		assertThat(pk, is(notNullValue()));
	}

}
