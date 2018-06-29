package no.difi.meldingsutveksling.noarkexchange;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.Decryptor;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.kvittering.EduDocumentFactory;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.mail.MailClient;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil.ARKIVMELDING_XML;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import static no.difi.meldingsutveksling.noarkexchange.logging.PutMessageResponseMarkers.markerFrom;

/**
 *
 */
@Component("recieveService")
@Slf4j
@WebService(portName = "ReceivePort", serviceName = "receive", targetNamespace = "", endpointInterface = "no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class IntegrajonspunktReceiveImpl implements SOAReceivePort, ApplicationContextAware {

    private static final String OKEY_TYPE = "OK";
    private static final String OK_TYPE = OKEY_TYPE;
    private static final Logger logger = LoggerFactory.getLogger(IntegrasjonspunktImpl.class);
    private static final String SBD_NAMESPACE = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader";
    private final TransportFactory transportFactory;
    private NoarkClient localNoark;
    private final Adresseregister adresseregisterService;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final IntegrasjonspunktProperties properties;
    private final IntegrasjonspunktNokkel keyInfo;
    private final ConversationService conversationService;
    private final MessageSender messageSender;
    private ApplicationContext context;

    @Autowired
    public IntegrajonspunktReceiveImpl(TransportFactory transportFactory,
                                       @Qualifier("localNoark") ObjectProvider<NoarkClient> localNoark,
                                       Adresseregister adresseregisterService,
                                       IntegrasjonspunktProperties properties,
                                       IntegrasjonspunktNokkel keyInfo,
                                       ServiceRegistryLookup serviceRegistryLookup,
                                       ConversationService conversationService,
                                       MessageSender messageSender) {

        this.transportFactory = transportFactory;
        this.localNoark = localNoark.getIfAvailable();
        this.adresseregisterService = adresseregisterService;
        this.properties = properties;
        this.keyInfo = keyInfo;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.conversationService = conversationService;
        this.messageSender = messageSender;
    }

    @Override
    public CorrelationInformation receive(@WebParam(name = "StandardBusinessDocument", targetNamespace = SBD_NAMESPACE, partName = "receiveResponse") StandardBusinessDocument standardBusinessDocument) {
        try {
            return forwardToNoarkSystem(standardBusinessDocument);
        } catch (MessageException e) {
            StandardBusinessDocumentWrapper documentWrapper = new StandardBusinessDocumentWrapper(standardBusinessDocument);
            Audit.error("Failed to deliver archive", markerFrom(documentWrapper), e);
            logger.error(markerFrom(documentWrapper),
                    e.getStatusMessage().getTechnicalMessage(), e);
            return new CorrelationInformation();
        }
    }

    public CorrelationInformation forwardToNoarkSystem(StandardBusinessDocument inputDocument) throws MessageException {
        StandardBusinessDocumentWrapper document = new StandardBusinessDocumentWrapper(inputDocument);

        try {
            adresseregisterService.validateCertificates(document);
            log.debug("Certificates validated", markerFrom(document));
        } catch (MessageException e) {
            Audit.error(e.getMessage(), markerFrom(document), e);
            throw e;
        }

        if (document.isReceipt()) {
            log.debug("Messagetype Receipt", markerFrom(document));
            return new CorrelationInformation();
        }

        Payload payload = document.getPayload();
        byte[] decryptedAsicPackage = decrypt(payload);
        EDUCore eduDocument;
        if (document.isNextMove()) {
            eduDocument = convertConversationToEducore(document);
        } else {
            eduDocument = convertAsicEntrytoEduDocument(decryptedAsicPackage);
            if (PayloadUtil.isAppReceipt(eduDocument.getPayload())) {
                Audit.info("AppReceipt extracted", markerFrom(document));
                Optional<Conversation> c = conversationService.registerStatus(eduDocument.getId(), MessageStatus.of(GenericReceiptStatus.LEST));
                c.ifPresent(conversationService::markFinished);
                if (!properties.getFeature().isForwardReceivedAppReceipts()) {
                    Audit.info("AppReceipt forwarding disabled - will not deliver to archive");
                    return new CorrelationInformation();
                }
            } else {
                Audit.info("EDU Document extracted", markerFrom(document));
            }
        }

        forwardToNoarkSystemAndSendReceipts(document, eduDocument);
        return new CorrelationInformation();
    }

    private EDUCore convertConversationToEducore(StandardBusinessDocumentWrapper doc) throws MessageException {
        Payload payload = doc.getPayload();
        byte[] decryptedAsicPackage = decrypt(payload);
        Arkivmelding arkivmelding = convertAsicEntryToArkivmelding(decryptedAsicPackage);
        ConversationResource cr = payload.getConversation();

        EDUCore eduCore = new EDUCoreFactory(serviceRegistryLookup).create(cr, arkivmelding, decryptedAsicPackage);
        Optional<Conversation> c = conversationService.registerStatus(eduCore.getId(), MessageStatus.of(GenericReceiptStatus.LEST));
        c.ifPresent(conversationService::markFinished);

        return eduCore;
    }

    byte[] decrypt(Payload payload) {
        byte[] cmsEncZip = DatatypeConverter.parseBase64Binary(payload.getContent());
        return new Decryptor(keyInfo).decrypt(cmsEncZip);
    }

    void forwardToNoarkSystemAndSendReceipts(StandardBusinessDocumentWrapper inputDocument, EDUCore eduCore) {
        PutMessageRequestType putMessage = new EDUCoreFactory(serviceRegistryLookup).createPutMessageFromCore(eduCore);
        PutMessageResponseType response = localNoark.sendEduMelding(putMessage);
        if (response == null || response.getResult() == null) {
            Audit.info("Empty response from archive", markerFrom(inputDocument));
        } else {
            AppReceiptType result = response.getResult();
            if (result.getType().equals(OK_TYPE)) {
                Audit.info("Delivered archive", markerFrom(response));
                Optional<Conversation> c = conversationService.registerStatus(inputDocument.getConversationId(),
                        MessageStatus.of(GenericReceiptStatus.INNKOMMENDE_LEVERT));
                c.ifPresent(conversationService::markFinished);
                sendReceiptOpen(inputDocument);
                if (localNoark instanceof MailClient && eduCore.getMessageType() != EDUCore.MessageType.APPRECEIPT) {
                    // Need to send AppReceipt manually in case receiver is mail
                    eduCore.swapSenderAndReceiver();
                    eduCore.setMessageType(EDUCore.MessageType.APPRECEIPT);
                    eduCore.setPayload(result);
                    messageSender.sendMessage(eduCore);
                }
            } else {
                Audit.error("Unexpected response from archive", markerFrom(response));
                logger.error(">>> archivesystem: " + response.getResult().getMessage().get(0).getText());
            }
        }

    }

    void sendReceiptOpen(StandardBusinessDocumentWrapper inputDocument) {
        EduDocument doc = EduDocumentFactory.createAapningskvittering(inputDocument.getMessageInfo(), keyInfo);
        sendReceipt(doc);
    }

    private void sendReceipt(EduDocument receipt) {
        Transport t = transportFactory.createTransport(receipt);
        t.send(context, receipt);
    }

    public EDUCore convertAsicEntrytoEduDocument(byte[] bytes) throws MessageException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if ("best_edu.xml".equals(entry.getName())) {
                    JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{EDUCore.class}, null);
                    Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
                    return unMarshaller.unmarshal(new StreamSource(zipInputStream), EDUCore.class).getValue();
                }
            }
        } catch (IOException | JAXBException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_EXTRACT_BEST_EDU);
        }
        throw new MessageException(StatusMessage.UNABLE_TO_EXTRACT_BEST_EDU);
    }

    public Arkivmelding convertAsicEntryToArkivmelding(byte[] bytes) throws MessageException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (ARKIVMELDING_XML.equals(entry.getName())) {
                    JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{Arkivmelding.class}, null);
                    Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
                    return unMarshaller.unmarshal(new StreamSource(zipInputStream), Arkivmelding.class).getValue();
                }
            }
        } catch (IOException | JAXBException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_EXTRACT_BEST_EDU);
        }
        throw new MessageException(StatusMessage.UNABLE_TO_EXTRACT_BEST_EDU);
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

    public IntegrasjonspunktProperties getProperties() {
        return properties;
    }

    public IntegrasjonspunktNokkel getKeyInfo() {
        return keyInfo;
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        this.context = ac;
    }
}
