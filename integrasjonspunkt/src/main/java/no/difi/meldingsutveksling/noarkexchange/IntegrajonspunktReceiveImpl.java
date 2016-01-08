package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.kvittering.DocumentSigner;
import no.difi.meldingsutveksling.kvittering.DocumentToDocumentConverter;
import no.difi.meldingsutveksling.kvittering.KvitteringFactory;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.kvittering.xsd.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.services.AdresseregisterVirksert;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingType;
import java.io.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipInputStream;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

/**
 *
 */

@Component("recieveService")
@WebService(portName = "ReceivePort", serviceName = "receive", targetNamespace = "", endpointInterface = "no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class IntegrajonspunktReceiveImpl implements SOAReceivePort {

    private Logger logger = LoggerFactory.getLogger(IntegrasjonspunktImpl.class);

    private static final int MAGIC_NR = 1024;
    public static final String SBD_NAMESPACE = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader";
    private EventLog eventLog = EventLog.create();

    @Autowired
    TransportFactory transportFactory;

    @Autowired
    private NoarkClient localNoark;

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private AdresseregisterVirksert adresseregisterService;

    @Autowired
    private IntegrasjonspunktConfiguration config;


    @Autowired
    private IntegrasjonspunktNokkel keyInfo;


    public IntegrajonspunktReceiveImpl() {
    }

    public CorrelationInformation receive(@WebParam(name = "StandardBusinessDocument", targetNamespace = SBD_NAMESPACE, partName = "receiveResponse") StandardBusinessDocument standardBusinessDocument) {

        try {
            return forwardToNoarkSystem(standardBusinessDocument);
        } catch (MessageException e) {
            logger.error(markerFrom(new StandardBusinessDocumentWrapper(standardBusinessDocument)),
                    e.getStatusMessage().getTechnicalMessage(), e);
            return new CorrelationInformation();
        }
    }

    public CorrelationInformation forwardToNoarkSystem(StandardBusinessDocument inputDocument) throws MessageException {
        StandardBusinessDocumentWrapper document = new StandardBusinessDocumentWrapper(inputDocument);
        adresseregisterService.validateCertificates(document);

        if (document.isReciept()) {
            logEvent(document, ProcessState.KVITTERING_MOTTATT);
            return new CorrelationInformation();
        }

        logEvent(document, ProcessState.SBD_RECIEVED);

        Payload payload = document.getPayload();

        byte[] decryptedAsicPackage = decrypt(payload);
        logEvent(document, ProcessState.DECRYPTION_SUCCESS);
        PutMessageRequestType eduDocument = convertAsicEntrytoEduDocument(decryptedAsicPackage);
        logEvent(document, ProcessState.BESTEDU_EXTRACTED);
        forwardToNoarkSystemAndSendReceipt(document, eduDocument);

        return new CorrelationInformation();
    }

    private byte[] decrypt(Payload payload) {
        byte[] cmsEncZip = DatatypeConverter.parseBase64Binary(payload.getContent());
        CmsUtil cmsUtil = new CmsUtil();
        return cmsUtil.decryptCMS(cmsEncZip, keyInfo.loadPrivateKey());
    }

    private PutMessageRequestType extractBestEdu(File bestEdu) throws MessageException {
        PutMessageRequestType putMessageRequestType;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
            Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
            putMessageRequestType = unMarshaller.unmarshal(new StreamSource(bestEdu), PutMessageRequestType.class).getValue();
        } catch (JAXBException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_EXTRACT_BEST_EDU);
        }
        return putMessageRequestType;
    }

    private void forwardToNoarkSystemAndSendReceipt(StandardBusinessDocumentWrapper inputDocument, PutMessageRequestType putMessageRequestType) {
        PutMessageResponseType response = localNoark.sendEduMelding(putMessageRequestType);
        if (response != null) {
            AppReceiptType result = response.getResult();
            if (null == result) {
                logEvent(inputDocument, ProcessState.ARCHIVE_NULL_RESPONSE);
            } else {
                logEvent(inputDocument, ProcessState.BEST_EDU_SENT);

            }
        } else {
            logEvent(inputDocument, ProcessState.ARCHIVE_NOT_AVAILABLE);
        }
    }

    private PutMessageRequestType convertAsicEntrytoEduDocument(byte[] bytes) throws MessageException {
        PutMessageRequestType returnValue;
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
            Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
            returnValue = unMarshaller.unmarshal(new StreamSource(zipInputStream), PutMessageRequestType.class).getValue();
        } catch (JAXBException | IOException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_EXTRACT_BEST_EDU);
        }
        return returnValue;
    }

    private void logEvent(StandardBusinessDocumentWrapper inputDocument, ProcessState processState) {
        eventLog.log(new Event().setProcessStates(processState)
                .setReceiver(inputDocument.getReceiverOrgNumber())
                .setSender(inputDocument.getSenderOrgNumber()));
    }

    public static Document createAapningskvittering(String orgNumberReceiver, String orgNumberSender, KeyPair keyPair) {
        return wrapAndSignReceipt(orgNumberReceiver, orgNumberSender, keyPair, KvitteringFactory.createAapningskvittering());
    }

    public static Document createLeveringsKvittering(String orgNumberReceiver, String orgNumberSender, KeyPair keyPair) {
        return wrapAndSignReceipt(orgNumberReceiver, orgNumberSender, keyPair, KvitteringFactory.createLeveringsKvittering());
    }

    private static Document wrapAndSignReceipt(String orgNumberReceiver, String orgNumberSender, KeyPair keyPair, Kvittering kvittering) {
        Document unsignedReceipt = new Document();
        StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader.Builder().
                from(new Organisasjonsnummer(orgNumberSender)).
                to(new Organisasjonsnummer(orgNumberReceiver))
                .build();
        unsignedReceipt.setStandardBusinessDocumentHeader(header);

        JAXBElement<Kvittering> jaxBKvittering = new ObjectFactory().createKvittering(kvittering);
        unsignedReceipt.setAny(jaxBKvittering);

        StandardBusinessDocument externalReceiptdocument = StandardBusinessDocumentFactory.create(unsignedReceipt);
        org.w3c.dom.Document xmlDoc = new DocumentToDocumentConverter(externalReceiptdocument).toDocument();
        org.w3c.dom.Document signedXmlDoc = DocumentSigner.sign(xmlDoc, keyPair);
        StandardBusinessDocument signedExternal = new DocumentToDocumentConverter(signedXmlDoc).getStandardBusinessDocument();
        return StandardBusinessDocumentFactory.create(signedExternal);
    }


    public TransportFactory getTransportFactory() {
        return transportFactory;
    }

    public void setTransportFactory(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    public NoarkClient getLocalNoark() {
        return localNoark;
    }

    public void setLocalNoark(NoarkClient localNoark) {
        this.localNoark = localNoark;
    }

    public IntegrasjonspunktConfiguration getConfig() {
        return config;
    }

    public void setConfig(IntegrasjonspunktConfiguration config) {
        this.config = config;
    }

    public IntegrasjonspunktNokkel getKeyInfo() {
        return keyInfo;
    }

    public void setKeyInfo(IntegrasjonspunktNokkel keyInfo) {
        this.keyInfo = keyInfo;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, JAXBException {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA");
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        final Document aapningskvittering = IntegrajonspunktReceiveImpl.createAapningskvittering("0110763434", "0110763434", kp);
        JAXBElement<Document> d = new no.difi.meldingsutveksling.domain.sbdh.ObjectFactory().createStandardBusinessDocument(aapningskvittering);
        System.out.println(toXml(d));
    }

    public static String toXml(JAXBElement<Document> doc) throws JAXBException {
        ByteArrayOutputStream baos;
        JAXBContext jc = JAXBContext.newInstance(Document.class, Kvittering.class);
        Marshaller marshaller = jc.createMarshaller();
        baos = new ByteArrayOutputStream();
        marshaller.marshal(doc, baos);
        return baos.toString();
    }
}
