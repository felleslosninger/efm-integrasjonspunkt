package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.services.AdresseregisterMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SendMessageTemplateTest {


	Dokumentpakker dokumentpakker;

	@Mock
	EventLog eventLogMock;

	SendMessageTemplateImpl subject;

	@Before
	public void setUp() {
        dokumentpakker = new Dokumentpakker();
		subject = new SendMessageTemplateImpl(dokumentpakker, new AdresseregisterMock()) {
			{
				setEventLog(eventLogMock);
			}
			@Override
			void sendSBD(StandardBusinessDocument sbd) throws IOException {

			}
		};

	}



	@Test
	public void testVerifySender() throws MeldingsUtvekslingRuntimeException {
		AddressType adresseType = new AddressType();
		adresseType.setOrgnr("960885406");
		KnutepunktContext context = new KnutepunktContext();
		subject.verifySender(adresseType, context);
		assertThat(context.getAvsender(), is(notNullValue()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testVerifySenderInvalid() throws MeldingsUtvekslingRuntimeException  {
		AddressType adresseType = new AddressType();
		adresseType.setOrgnr("12345678");
		KnutepunktContext context = new KnutepunktContext();
		subject.verifySender(adresseType, context);
	}

	@Test
	public void testVerifyRecipient() throws MeldingsUtvekslingRuntimeException  {
		AddressType adresseType = new AddressType();
		adresseType.setOrgnr("960885406");
		KnutepunktContext context = new KnutepunktContext();
		subject.verifyRecipient(adresseType, context);
		assertThat(context.getMottaker(), is(notNullValue()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testVerifyRecipientInvalid()  throws MeldingsUtvekslingRuntimeException  {
		AddressType adresseType = new AddressType();
		adresseType.setOrgnr("12345678");
		KnutepunktContext context = new KnutepunktContext();
		subject.verifyRecipient(adresseType, context);
	}

	@Test
	public void testFindPrivateKey()throws MeldingsUtvekslingRuntimeException  {
		PrivateKey pk = subject.findPrivateKey();
		assertThat(pk, is(notNullValue()));
	}

    @Test
    public  void testSendMessageWithFileExample() {
        File file = new File(getClass().getClassLoader().getResource("putmessageEksampel.xml").getFile());
        PutMessageRequestType putMessageRequestType ;
        try{
            JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
            Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
            putMessageRequestType =   unMarshaller.unmarshal(new StreamSource(file) , PutMessageRequestType.class).getValue();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        subject.sendMessage(putMessageRequestType);
    }
}
