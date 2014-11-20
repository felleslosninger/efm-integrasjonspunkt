package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.adresseregmock.AdressRegisterFactory;
import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.BestEduMessage;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.SBD;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.apache.commons.codec.binary.Base64;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public abstract class SendMessageTemplate {

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

		return new SBD(dokumentpakker.pakkDokumentISbd(new BestEduMessage(os.toByteArray()), context.getAvsender(), context.getMottaker()));
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
		final String avsenderPrivateKey = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAM7w0IG4Cj7Pr7KH"
				+ "DD4fM3LFlzvN5Pju2bnNnsDRhoR7wsK+xxVXLcsl+kScNxNIjy2+6BaR+pniM4bA" + "TqK1fjrN2oEZ6MinITHJzuQYp/MTg4+afCV4vKXmkl+siopjjwWWD7a4FhP6TQfj"
				+ "gcApPIEf1iwo8bghL2tQGwUjohLFAgMBAAECgYAling44Bszs9eKyocFCgH6UzAR" + "UFO2eRYUZ+Hh1uDRTeZSD+vryinrjZMuOSygmewnf1d5KLhOjEOOsXpSeBxS2RYo"
				+ "csteW78txCRsSEJo7i9ASmw7w0vvN0tVqTCbjNokI8xS6Kn+GH96vMCNq4ImuBkg" + "zWuaDA6GP/FQorSCzQJBAOvxwJIWvc44aSTceYYOVQUJZ3b9a6y2rpuvdcpuPwJL"
				+ "Y2YbPq5SlQAhsh5Yss2d8aAGvaDXbZOPVZyvCaeAMf8CQQDgh+4uNMQAu556DNPa" + "GDl3JI4JmgZl8bbiRQRFU5h02AkoNv03izyafOqpl61X1WCeHBx1nn2ivIXJ/0ub"
				+ "X3M7AkBshC3rguYdOLizKWwDCgh0XpTllzy0nPjFxfdI+VeleILo7VLw3i6Fdvnz" + "Fxx1kVUWIsOIfEx7d4sKmz63eTCFAkB+MafabGmlB84gRsljEK5rmi4Ck4D5Fwt0"
				+ "zNmDpWJQeYNcCNv0tdsP8RlqzAbvEMxG0QHl0XhHWLHRQB1cbB81AkEAtnzxewKS" + "P40rj3bkKSj8tuSOBbnpzWp93P8FFkyHNZCKbEArf89gYHLopwoe3kixp3u8QiXl"
				+ "s2TPH0mjyb7Keg==";

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(avsenderPrivateKey));
		KeyFactory kf = null;
		try {
			kf = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		try {
			return kf.generatePrivate(keySpec);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

}
