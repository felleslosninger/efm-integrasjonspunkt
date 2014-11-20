package no.difi.meldingsutveksling.noarkexchange;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.security.PrivateKey;

import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.SBD;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.services.AdresseregisterMock;

import org.junit.Test;

public class SendMessageTemplateTest {

	SendMessageTemplate subject = new SendMessageTemplate(new Dokumentpakker() {
		@Override
		public byte[] pakkDokumentISbd(ByteArrayFile document, Avsender avsender, Mottaker mottaker, String conversationId) {
			return new byte[2];
		}
	}) {
		{
			setAdresseregister(new AdresseregisterMock());
		}

		@Override
		void sendSBD(SBD sbd) throws IOException {
		}
	};

	@Test
	public void testCreateSBD() {
		subject.createSBD(new PutMessageRequestType(), new KnutepunktContext());
	}

	@Test
	public void testVerifySender() throws Exception {
		AddressType adresseType = new AddressType();
		adresseType.setOrgnr("960885406");
		KnutepunktContext context = new KnutepunktContext();
		subject.verifySender(adresseType, context);
		assertThat(context.getAvsender(), is(notNullValue()));
	}

	@Test(expected = IllegalArgumentException.class)
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

	@Test(expected = IllegalArgumentException.class)
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
