package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.SBD;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.services.AdresseregisterMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.security.PrivateKey;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SendMessageTemplateTest {

	@Mock
	Dokumentpakker dokumentpakker;

	@Mock
	EventLog eventLogMock;

	SendMessageTemplate subject;

	@Before
	public void setUp() {
		subject = new SendMessageTemplate(dokumentpakker, new AdresseregisterMock()) {
			{
				setEventLog(eventLogMock);
			}
			@Override
			void sendSBD(SBD sbd) throws IOException {
			}
		};
		Mockito.when(
				dokumentpakker.pakkDokumentISbd(Mockito.any(ByteArrayFile.class), Mockito.any(Avsender.class), Mockito.any(Mottaker.class),
						Mockito.any(String.class),"BEST/EDU")).thenReturn(new byte[1]);
	}

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
	}

	@Test
	public void testFindPrivateKey() throws Exception {
		PrivateKey pk = subject.findPrivateKey();
		assertThat(pk, is(notNullValue()));
	}

	@Test
	public void testSendMessage() throws Exception {
		PutMessageRequestType request = new PutMessageRequestType();
		request.setEnvelope(new EnvelopeType());
		request.getEnvelope().setReceiver(new AddressType());
		request.getEnvelope().setSender(new AddressType());
		request.getEnvelope().getReceiver().setOrgnr("960885406");
		request.getEnvelope().getSender().setOrgnr("960885406");
		
		assertThat(subject.sendMessage(request).getResult(), is(nullValue()));
		
	}

}
