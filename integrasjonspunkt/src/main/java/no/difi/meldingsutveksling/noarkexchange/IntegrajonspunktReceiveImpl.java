package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.kvittering.KvitteringFactory;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.services.AdresseregisterVirksert;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;

/**
 *
 */

@Component("recieveService")
@WebService(portName = "ReceivePort", serviceName = "receive", targetNamespace = "", endpointInterface = "no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class IntegrajonspunktReceiveImpl implements SOAReceivePort {

    private static Logger logger = LoggerFactory.getLogger(IntegrasjonspunktImpl.class);
    private static final int MAGIC_NR = 1024;
    private static final String SBD_NAMESPACE = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader";
    private EventLog eventLog = EventLog.create();
    private TransportFactory transportFactory;
    private NoarkClient localNoark;
    private MessageSender messageSender;
    private AdresseregisterVirksert adresseregisterService;
    private IntegrasjonspunktConfiguration config;
    private IntegrasjonspunktNokkel keyInfo;

    @Autowired
    public IntegrajonspunktReceiveImpl(TransportFactory transportFactory,
                                       NoarkClient localNoark,
                                       MessageSender messageSender,
                                       AdresseregisterVirksert adresseregisterService,
                                       IntegrasjonspunktConfiguration config,
                                       IntegrasjonspunktNokkel keyInfo) {

        this.transportFactory = transportFactory;
        this.localNoark = localNoark;
        this.messageSender = messageSender;
        this.adresseregisterService = adresseregisterService;
        this.config = config;
        this.keyInfo = keyInfo;
    }

    public CorrelationInformation receive(@WebParam(name = "StandardBusinessDocument", targetNamespace = SBD_NAMESPACE, partName = "receiveResponse") StandardBusinessDocument standardBusinessDocument) {
        try {
            return  forwardToNoarkSystem(standardBusinessDocument);
        } catch (MessageException e) {
            StandardBusinessDocumentWrapper documentWrapper = new StandardBusinessDocumentWrapper(standardBusinessDocument);
            Audit.error("Message could not be sent to archive system", markerFrom(documentWrapper));
            logger.error(markerFrom(documentWrapper),
                    e.getStatusMessage().getTechnicalMessage(), e);
            return new CorrelationInformation();
        }
    }

    public CorrelationInformation forwardToNoarkSystem(StandardBusinessDocument inputDocument) throws MessageException {
        StandardBusinessDocumentWrapper document = new StandardBusinessDocumentWrapper(inputDocument);
        adresseregisterService.validateCertificates(document);
        Audit.info("Sender and recievers certificates are validated. Processing contents...", markerFrom(new StandardBusinessDocumentWrapper(inputDocument)));
        if (document.isReciept()) {
            Audit.info("Received message is a receipt. Finished", markerFrom(document));
            logEvent(document, ProcessState.KVITTERING_MOTTATT);
            return new CorrelationInformation();
        }

        logEvent(document, ProcessState.SBD_RECIEVED);
        Payload payload = document.getPayload();
        byte[] decryptedAsicPackage = decrypt(payload);
        logEvent(document, ProcessState.DECRYPTION_SUCCESS);
        PutMessageRequestType eduDocument;
        try {
            eduDocument = convertAsicEntrytoEduDocument(decryptedAsicPackage);
        } catch (IOException | JAXBException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_EXTRACT_BEST_EDU);
        }
        Audit.info("Successfully extracted BEST/EDU document from message payload", markerFrom(document));
        forwardToNoarkSystemAndSendReceipt(document, eduDocument);
        return new CorrelationInformation();
    }

    private byte[] decrypt(Payload payload) {
        byte[] cmsEncZip = DatatypeConverter.parseBase64Binary(payload.getContent());
        CmsUtil cmsUtil = new CmsUtil();
        return cmsUtil.decryptCMS(cmsEncZip, keyInfo.loadPrivateKey());
    }

    private void forwardToNoarkSystemAndSendReceipt(StandardBusinessDocumentWrapper inputDocument, PutMessageRequestType putMessageRequestType) {
        PutMessageResponseType response = localNoark.sendEduMelding(putMessageRequestType);
        if (response != null) {
            AppReceiptType result = response.getResult();
            if (null == result) {
                logEvent(inputDocument, ProcessState.ARCHIVE_NULL_RESPONSE);
            } else {
                Audit.info("Document successfully sent to NOARK system. Sending receipt...", markerFrom(inputDocument));
                sendReceipt(inputDocument);
                logEvent(inputDocument, ProcessState.BEST_EDU_SENT);
            }
        } else {
            logEvent(inputDocument, ProcessState.ARCHIVE_NOT_AVAILABLE);
        }
    }

    private void sendReceipt(StandardBusinessDocumentWrapper inputDocument) {
        Document doc = KvitteringFactory.createAapningskvittering(inputDocument.getSenderOrgNumber(),
                inputDocument.getReceiverOrgNumber(), inputDocument.getJournalPostId(),
                inputDocument.getConversationId(), keyInfo.getKeyPair());
        Transport t = transportFactory.createTransport(doc);
        t.send(config.getConfiguration(), doc);
    }

    private PutMessageRequestType convertAsicEntrytoEduDocument(byte[] bytes) throws MessageException, IOException, JAXBException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().equals("best_edu.xml")) {
                    JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
                    Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
                    return unMarshaller.unmarshal(new StreamSource(zipInputStream), PutMessageRequestType.class).getValue();
                }
            }
        }
        throw new MessageException(StatusMessage.UNABLE_TO_EXTRACT_BEST_EDU);
    }

    private void logEvent(StandardBusinessDocumentWrapper inputDocument, ProcessState processState) {
        eventLog.log(new Event().setProcessStates(processState)
                .setReceiver(inputDocument.getReceiverOrgNumber())
                .setSender(inputDocument.getSenderOrgNumber()));
    }

    public TransportFactory getTransportFactory() {
        return transportFactory;
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

    public IntegrasjonspunktNokkel getKeyInfo() {
        return keyInfo;
    }
}
