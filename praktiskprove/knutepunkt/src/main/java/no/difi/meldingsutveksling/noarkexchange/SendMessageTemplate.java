package no.difi.meldingsutveksling.noarkexchange;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.domain.*;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.eventlog.ProcessState;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.services.AdresseregisterMock;
import no.difi.meldingsutveksling.services.AdresseregisterService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import java.io.*;
import java.nio.charset.Charset;
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

    @Autowired
    AdresseregisterService adresseregister;

    Dokumentpakker dokumentpakker;

    public SendMessageTemplate(Dokumentpakker dokumentpakker, AdresseregisterService adresseregister) {
        this.dokumentpakker = dokumentpakker;
        this.adresseregister = adresseregister;
    }

    public SendMessageTemplate() {
        this(new Dokumentpakker(), new AdresseregisterMock());
    }

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

        return new SBD(dokumentpakker.pakkDokumentISbd(new BestEduMessage(os.toByteArray()), context.getAvsender(), context.getMottaker(), UUID.randomUUID()
                .toString()));
    }

    abstract void sendSBD(SBD sbd) throws IOException;

    boolean verifySender(AddressType sender, KnutepunktContext context) {
        try {
            Avsender avsender = null;
            Certificate sertifikat = (Certificate) adresseregister.getCertificate(sender.getOrgnr());
            if (sertifikat == null)
                throw new InvalidSender();
            avsender = Avsender.builder(new Organisasjonsnummer(sender.getOrgnr()), new Noekkelpar(findPrivateKey(), sertifikat)).build();
            context.setAvsender(avsender);
        } catch (IllegalArgumentException e) {
            throw new InvalidSender();
        }
        return true;
    }

    boolean verifyRecipient(AddressType receiver, KnutepunktContext context) {
        try {
            PublicKey mottakerpublicKey = adresseregister.getPublicKey(receiver.getOrgnr());
            if (mottakerpublicKey == null)
                throw new InvalidReceiver();
            Mottaker mottaker = new Mottaker(new Organisasjonsnummer(receiver.getOrgnr()), mottakerpublicKey);
            context.setMottaker(mottaker);
        } catch (IllegalArgumentException e) {
            throw new InvalidReceiver();
        }
        return true;
    }

    public PutMessageResponseType sendMessage(PutMessageRequestType message) {
        KnutepunktContext context = new KnutepunktContext();
        try {
            verifySender(message.getEnvelope().getSender(), context);
            verifyRecipient(message.getEnvelope().getReceiver(), context);
            eventLog.log(createOkStateEvent(message, ProcessState.SIGNATURE_VALIDATED));

        } catch (InvalidSender | InvalidReceiver e) {
            Event errorEvent = createErrorEvent(message, ProcessState.SIGNATURE_VALIDATION_ERROR, e);
            eventLog.log(errorEvent);
            return createErrorResponse();
        }

        SBD sbd = createSBD(message, context);
        try {
            sendSBD(sbd);
        } catch (IOException e) {
            eventLog.log(createErrorEvent(message, ProcessState.MESSAGE_SEND_FAIL, e));
            return createErrorResponse();
        }
        eventLog.log(createOkStateEvent(message, ProcessState.SBD_SENT));
        return new PutMessageResponseType();
    }

    private PutMessageResponseType createErrorResponse() {
        PutMessageResponseType response = new PutMessageResponseType();
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType("ERROR");
        response.setResult(receipt);
        return response;
    }

    PrivateKey findPrivateKey() {
        PrivateKey key = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("knutepunkt_privatekey.pkcs8"),
                Charset.forName("UTF-8")))) {
            StringBuilder builder = new StringBuilder();
            boolean inKey = false;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!inKey && line.startsWith("-----BEGIN ") && line.endsWith(" PRIVATE KEY-----")) {
                    inKey = true;
                } else {
                    if (line.startsWith("-----END ") && line.endsWith(" PRIVATE KEY-----")) {
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

    public void setAdresseregister(AdresseregisterService adresseregister) {
        this.adresseregister = adresseregister;
    }

    public void setEventLog(EventLog eventLog) {
        this.eventLog = eventLog;
    }

    private Event createErrorEvent(PutMessageRequestType anyOject, ProcessState state, Exception e) {
        XStream xs = new XStream();
        Event event = new Event();
        event.setSender(anyOject.getEnvelope().getSender().getOrgnr());
        event.setReceiver(anyOject.getEnvelope().getReceiver().getOrgnr());
        event.setExceptionMessage(event.getMessage());
        event.setTimeStamp(System.currentTimeMillis());
        event.setProcessStates(state);
        event.setMessage(xs.toXML(anyOject));
        return event;
    }

    private Event createOkStateEvent(PutMessageRequestType anyOject, ProcessState state) {
        XStream xs = new XStream();
        Event event = new Event();
        event.setSender(anyOject.getEnvelope().getSender().getOrgnr());
        event.setReceiver(anyOject.getEnvelope().getReceiver().getOrgnr());
        event.setMessage(xs.toXML(anyOject));
        event.setTimeStamp(System.currentTimeMillis());
        event.setProcessStates(state);
        return event;
    }

}

