package no.difi.meldingsutveksling.noarkexchange;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingType;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.kvittering.EduDocumentFactory;
import no.difi.meldingsutveksling.logging.Audit;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import no.difi.meldingsutveksling.logging.MoveLogMarkers;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component("recieveService")
@WebService(portName = "ReceivePort", serviceName = "receive", targetNamespace = "", endpointInterface = "no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class IntegrajonspunktReceiveImpl implements SOAReceivePort {

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
    private final ApplicationContext context;

    @Autowired
    public IntegrajonspunktReceiveImpl(TransportFactory transportFactory,
            NoarkClient localNoark,
            Adresseregister adresseregisterService,
            IntegrasjonspunktProperties properties,
            IntegrasjonspunktNokkel keyInfo,
            ServiceRegistryLookup serviceRegistryLookup,
            ApplicationContext context) {

        this.transportFactory = transportFactory;
        this.localNoark = localNoark;
        this.adresseregisterService = adresseregisterService;
        this.properties = properties;
        this.keyInfo = keyInfo;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.context = context;
    }

    @Override
    public CorrelationInformation receive(@WebParam(name = "StandardBusinessDocument", targetNamespace = SBD_NAMESPACE, partName = "receiveResponse") StandardBusinessDocument standardBusinessDocument) {
        MDC.put(MoveLogMarkers.KEY_ORGANISATION_NUMBER, properties.getOrg().getNumber());
        try {
            return forwardToNoarkSystem(standardBusinessDocument);
        } catch (MessageException e) {
            StandardBusinessDocumentWrapper documentWrapper = new StandardBusinessDocumentWrapper(standardBusinessDocument);
            Audit.error("Failed to deliver archive", markerFrom(documentWrapper));
            logger.error(markerFrom(documentWrapper),
                    e.getStatusMessage().getTechnicalMessage(), e);
            return new CorrelationInformation();
        }
    }

    public CorrelationInformation forwardToNoarkSystem(StandardBusinessDocument inputDocument) throws MessageException {
        StandardBusinessDocumentWrapper document = new StandardBusinessDocumentWrapper(inputDocument);

        try {
            adresseregisterService.validateCertificates(document);
            Audit.info("Certificates validated", markerFrom(document));
        } catch (MessageException e) {
            Audit.error(e.getMessage(), markerFrom(document));
            throw e;
        }

        if (document.isReciept()) {
            Audit.info("Messagetype Receipt", markerFrom(document));
            return new CorrelationInformation();
        }

        // TODO: remove? or move to send leveringskvittering i internal queue?
        Payload payload = document.getPayload();
        byte[] decryptedAsicPackage = decrypt(payload);
        EDUCore eduDocument;
        try {
            eduDocument = convertAsicEntrytoEduDocument(decryptedAsicPackage);
            if (PayloadUtil.isAppReceipt(eduDocument.getPayload())) {
                Audit.info("AppReceipt extracted", markerFrom(document));
            } else {
                Audit.info("EDU Document extracted", markerFrom(document));
            }

        } catch (IOException | JAXBException e) {
            Audit.error("Failed to extract EDUdocument", markerFrom(document));
            throw new MessageException(e, StatusMessage.UNABLE_TO_EXTRACT_BEST_EDU);
        }

        forwardToNoarkSystemAndSendReceipts(document, eduDocument);
        return new CorrelationInformation();
    }

    public byte[] decrypt(Payload payload) {
        byte[] cmsEncZip = DatatypeConverter.parseBase64Binary(payload.getContent());
        CmsUtil cmsUtil = new CmsUtil();
        return cmsUtil.decryptCMS(cmsEncZip, keyInfo.loadPrivateKey());
    }

    public void forwardToNoarkSystemAndSendReceipts(StandardBusinessDocumentWrapper inputDocument, EDUCore eduCore) {
        PutMessageRequestType putMessage = new EDUCoreFactory(serviceRegistryLookup).createPutMessageFromCore(eduCore);
        PutMessageResponseType response = localNoark.sendEduMelding(putMessage);
        if (response == null || response.getResult() == null) {
            Audit.info("Empty response from archive", markerFrom(inputDocument));
        } else {
            AppReceiptType result = response.getResult();
            if (result.getType().equals(OK_TYPE)) {
                Audit.info("Delivered archive", markerFrom(response));
                sendReceiptOpen(inputDocument);
            } else {
                Audit.error("Unexpected response from archive", markerFrom(response));
                System.out.println(">>> archivesystem: " + response.getResult().getMessage().get(0).getText());
            }
        }

    }

    public void sendReceiptOpen(StandardBusinessDocumentWrapper inputDocument) {
        EduDocument doc = EduDocumentFactory.createAapningskvittering(inputDocument.getMessageInfo(), keyInfo.getKeyPair());
        sendReceipt(doc);
    }

    private void sendReceipt(EduDocument receipt) {
        Transport t = transportFactory.createTransport(receipt);
        t.send(context, receipt);
    }

    public EDUCore convertAsicEntrytoEduDocument(byte[] bytes) throws MessageException, IOException, JAXBException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if ("best_edu.xml".equals(entry.getName())) {
                    JAXBContext jaxbContext = JAXBContext.newInstance(EDUCore.class);
                    Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
                    return unMarshaller.unmarshal(new StreamSource(zipInputStream), EDUCore.class).getValue();
                }
            }
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
}
