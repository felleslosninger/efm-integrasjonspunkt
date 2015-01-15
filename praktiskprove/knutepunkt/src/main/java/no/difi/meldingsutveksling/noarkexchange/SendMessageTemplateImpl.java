package no.difi.meldingsutveksling.noarkexchange;


import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.BestEduMessage;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.*;
import no.difi.meldingsutveksling.services.AdresseregisterMock;
import no.difi.meldingsutveksling.services.AdresseregisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.UUID;

@Component
public abstract class SendMessageTemplateImpl {

    private static final String JP_ID = "jpId";
    private static final String DATA = "data";
    @Autowired
    EventLog eventLog;

    @Autowired
    AdresseregisterService adresseregister;

    private final Dokumentpakker dokumentpakker;

    public SendMessageTemplateImpl(Dokumentpakker dokumentpakker, AdresseregisterService adresseregister) {
        this.dokumentpakker = dokumentpakker;
        this.adresseregister = adresseregister;
    }

    public SendMessageTemplateImpl() {
        this(new Dokumentpakker(), new AdresseregisterMock());
    }

    StandardBusinessDocument createSBD(PutMessageRequestType sender, KnutepunktContext context) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(new ObjectFactory().createPutMessageRequest(sender), os);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return dokumentpakker.pakkDokumentIStandardBusinessDocument(new BestEduMessage(os.toByteArray()), context.getAvsender(), context.getMottaker(), UUID.randomUUID()
                .toString(), "melding");
    }

    abstract void sendSBD(StandardBusinessDocument sbd) throws IOException;


    boolean verifySender(AddressType sender, KnutepunktContext context) {
        if (sender == null) {
            return false;
        }
        try {
            Avsender avsender;
            Certificate sertifikat = adresseregister.getCertificate(sender.getOrgnr());
            if (sertifikat == null) {
                throw new InvalidSender(new RuntimeException("invalid sender"));
            }
            avsender = Avsender.builder(new Organisasjonsnummer(sender.getOrgnr()), new Noekkelpar(findPrivateKey(), sertifikat)).build();
            context.setAvsender(avsender);
        } catch (IllegalArgumentException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
            throw new InvalidSender(new RuntimeException("invalid sender"));
        }
        return true;
    }

    boolean verifyRecipient(AddressType receiver, KnutepunktContext context) {
        if (receiver == null) {
            return false;
        }
        try {
            Certificate sertifikat = adresseregister.getCertificate(receiver.getOrgnr());
            if (sertifikat == null)
                throw new InvalidReceiver(new RuntimeException("invalid receiver"));
            Mottaker mottaker = new Mottaker(new Organisasjonsnummer(receiver.getOrgnr()), (X509Certificate) sertifikat);
            context.setMottaker(mottaker);
        } catch (IllegalArgumentException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
            throw new InvalidReceiver(new RuntimeException("invalid receiver"));
        }
        return true;
    }

    public PutMessageResponseType sendMessage(PutMessageRequestType message) {

        String conversationId = message.getEnvelope().getConversationId();
        String journalPostId = getJpId(message);

        KnutepunktContext context = new KnutepunktContext();
        context.setJpId(journalPostId);

        EnvelopeType envelope = message.getEnvelope();
        if (envelope == null) {
            return createErrorResponse("Missing envelope");
        }
        if (verifyRecipient(message.getEnvelope().getReceiver(), context)) {
            return createErrorResponse("invalid recipient");
        }
        if (verifySender(message.getEnvelope().getSender(), context)) {
            return createErrorResponse("invalid sender");
        }

        eventLog.log(new Event(ProcessState.SIGNATURE_VALIDATED));

        StandardBusinessDocument sbd = createSBD(message, context);

        Scope item = sbd.getStandardBusinessDocumentHeader().getBusinessScope().getScope().get(0);
        String hubCid = item.getInstanceIdentifier();
        eventLog.log(new Event().setJpId(journalPostId).setArkiveConversationId(conversationId).setHubConversationId(hubCid).setProcessStates(ProcessState.CONVERSATION_ID_LOGGED));

        try {
            sendSBD(sbd);
        } catch (IOException e) {
            eventLog.log(createErrorEvent(message, e));
            return createErrorResponse("Error on message send, check event log.");
        }
        eventLog.log(createOkStateEvent(message));
        return new PutMessageResponseType();
    }

    private String getJpId(PutMessageRequestType message) {
        Document document = getDocument(message);
        NodeList messageElement = document.getElementsByTagName(JP_ID);
        if (messageElement.getLength() == 0) {
            throw new MeldingsUtvekslingRuntimeException("no " + JP_ID + " element in document ");
        }
        return messageElement.item(0).getTextContent();
    }

    private Document getDocument(PutMessageRequestType message) throws MeldingsUtvekslingRuntimeException {
        DocumentBuilder documentBuilder = getDocumentBuilder();
        Element element = (Element) message.getPayload();
        NodeList nodeList = element.getElementsByTagName(DATA);
        if (nodeList.getLength() == 0) {
            throw new MeldingsUtvekslingRuntimeException("no " + DATA + "  element in payload");
        }
        Node payloadData = nodeList.item(0);
        String payloadDataTextContent = payloadData.getTextContent();
        Document document;

        try {
            document = documentBuilder.parse(new InputSource(new ByteArrayInputStream(payloadDataTextContent.getBytes("utf-8"))));
        } catch (SAXException | IOException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return document;
    }

    private DocumentBuilder getDocumentBuilder() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return builder;
    }

    private PutMessageResponseType createErrorResponse() {
        return createErrorResponse("ERROR_INVALID_OR_MISSING_SENDER");
    }

    private PutMessageResponseType createErrorResponse(String message) {
        PutMessageResponseType response = new PutMessageResponseType();
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType(message);
        response.setResult(receipt);
        return response;
    }


    //todo refactor
    PrivateKey findPrivateKey() {
        PrivateKey key;
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
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return key;
    }

    public void setAdresseregister(AdresseregisterService adresseregister) {
        this.adresseregister = adresseregister;
    }

    public void setEventLog(EventLog eventLog) {
        this.eventLog = eventLog;
    }

    private Event createErrorEvent(PutMessageRequestType anyOject, Exception e) {
        XStream xs = new XStream();
        Event event = new Event();
        event.setSender(anyOject.getEnvelope().getSender().getOrgnr());
        event.setReceiver(anyOject.getEnvelope().getReceiver().getOrgnr());
        event.setExceptionMessage(event.getMessage());
        event.setProcessStates(ProcessState.MESSAGE_SEND_FAIL);
        event.setMessage(xs.toXML(anyOject));
        return event;
    }

    private Event createOkStateEvent(PutMessageRequestType anyOject) {
        XStream xs = new XStream();
        Event event = new Event();
        event.setSender(anyOject.getEnvelope().getSender().getOrgnr());
        event.setReceiver(anyOject.getEnvelope().getReceiver().getOrgnr());
        event.setMessage(xs.toXML(anyOject));
        event.setProcessStates(ProcessState.SBD_SENT);
        return event;
    }

}

