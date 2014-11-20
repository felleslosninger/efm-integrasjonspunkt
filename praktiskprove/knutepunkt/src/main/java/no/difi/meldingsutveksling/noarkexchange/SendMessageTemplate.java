package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.adresseregmock.AdressRegisterFactory;
import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.domain.*;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.UUID;

@Component
public abstract class SendMessageTemplate {

    @Autowired
    EventLog eventLog;

	private EventLog eventLog = EventLog.create();

	SBD createSBD(PutMessageRequestType sender, KnutepunktContext context) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(new ObjectFactory().createPutMessageRequest(sender), os);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		Dokumentpakker dokumentpakker = new Dokumentpakker();

		return new SBD(dokumentpakker.pakkDokumentISbd(new BestEduMessage(os.toByteArray()), context.getAvsender(), context.getMottaker(), UUID.randomUUID().toString()));
	}

	abstract void sendSBD(SBD sbd) throws IOException;

	boolean verifySender(AddressType sender, KnutepunktContext context) {
		Avsender avsender = null;
		avsender = Avsender.builder(new Organisasjonsnummer(sender.getOrgnr()),
				new Noekkelpar(findPrivateKey(), (Certificate) AdressRegisterFactory.createAdressRegister().getCertificate(sender.getOrgnr())))
				.build();
		context.setAvsender(avsender);
		
		return true;
	}


	boolean verifyRecipient(AddressType receiver, KnutepunktContext context) {
		final PublicKey mottakerpublicKey = AdressRegisterFactory.createAdressRegister().getPublicKey(receiver.getOrgnr());
		Mottaker mottaker = new Mottaker(new Organisasjonsnummer(receiver.getOrgnr()), mottakerpublicKey);
		context.setMottaker(mottaker);
		
		return true;
	}

	public PutMessageResponseType sendMessage(PutMessageRequestType message) {


		KnutepunktContext context = new KnutepunktContext();
		
		verifySender(message.getEnvelope().getSender(), context);
		eventLog.log(new Event());

		verifyRecipient(message.getEnvelope().getReceiver(), context);
		eventLog.log(new Event());

		SBD sbd = createSBD(message, context);
		eventLog.log(new Event());

		try {
			sendSBD(sbd);
		} catch (IOException e) {
			eventLog.log(new Event());
		}
		eventLog.log(new Event());
		return new PutMessageResponseType();
	}
	
	PrivateKey findPrivateKey(){
		 PrivateKey key = null;
	        try(InputStream is = PrivateKeyUtil.class.getClassLoader().getResourceAsStream("knutepunkt_privatekey.pkcs8")) {
	            BufferedReader br = new BufferedReader(new InputStreamReader(is));
	            StringBuilder builder = new StringBuilder();
	            boolean inKey = false;
	            for (String line = br.readLine(); line != null; line = br.readLine()) {
	                if (!inKey &&  line.startsWith("-----BEGIN ") &&
	                        line.endsWith(" PRIVATE KEY-----") ) {
	                        inKey = true;
	                } else {
	                    if (line.startsWith("-----END ") &&
	                            line.endsWith(" PRIVATE KEY-----")) {
	                        inKey = false;
	                    }
	                    builder.append(line);
	                }
	            }

	            byte[] encoded = DatatypeConverter.parseBase64Binary(builder.toString());
	            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
	            KeyFactory kf = KeyFactory.getInstance("RSA");
	            key = kf.generatePrivate(keySpec);

	        } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
	        	throw new RuntimeException(e);
	        }
	        return key;
	}

}
