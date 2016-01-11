package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.logging.Audit;
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
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
public class IntegrajonspunktReceiveImpl  implements SOAReceivePort {

    private Logger logger = LoggerFactory.getLogger(IntegrasjonspunktImpl.class);

    private static final int MAGIC_NR = 1024;
    public static final String SBD_NAMESPACE = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader";
    private EventLog eventLog = EventLog.create();

    @Autowired
    TransportFactory transportFactory;

    @Autowired
    private NoarkClient localNoark;

    @Autowired
    private AdresseregisterVirksert adresseregisterService;

    @Autowired
    private IntegrasjonspunktConfiguration config;


    @Autowired
    private IntegrasjonspunktNokkel keyInfo;


    public IntegrajonspunktReceiveImpl() {
    }

    public CorrelationInformation receive(@WebParam(name = "StandardBusinessDocument", targetNamespace = SBD_NAMESPACE, partName = "receiveResponse") StandardBusinessDocument standardBusinessDocument) {
        Audit.info("Message recieved", standardBusinessDocument);
        try {
            return forwardToNoarkSystem(standardBusinessDocument);
        } catch (MessageException e) {
            Audit.error("Message could not be sent to archive system", standardBusinessDocument);
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

        byte[] decryptedPayload = decrypt(payload);
        logEvent(document, ProcessState.DECRYPTION_SUCCESS);
        File decompressedPayload;
        decompressedPayload = decompressToFile(decryptedPayload);
        logEvent(document, ProcessState.BESTEDU_EXTRACTED);
        PutMessageRequestType putMessageRequestType = extractBestEdu(decompressedPayload);
        forwardToNoarkSystemAndSendReceipt(document, putMessageRequestType);

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

    private File decompressToFile(byte[] bytes) throws MessageException {
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes));
        ZipEntry zipEntry;
        String outputFolder = System.getProperty("user.home") + File.separator + "testToRemove" +
                File.separator + "Zip Output";
        File newFile = null;
        try {
            zipEntry = zipInputStream.getNextEntry();
            while (null != zipEntry) {
                String fileName = zipEntry.getName();
                if ("edu_test.xml".equals(fileName)) {

                    newFile = new File(outputFolder + File.separator + fileName);
                    FileOutputStream fos;
                    new File(newFile.getParent()).mkdirs();
                    try {
                        fos = new FileOutputStream(newFile);
                    } catch (FileNotFoundException e) {
                        throw new MessageException(e, StatusMessage.UNABLE_TO_EXTRACT_ZIP_CONTENTS);
                    }
                    byte[] bufbyte = new byte[MAGIC_NR];
                    int len;
                    while ((len = zipInputStream.read(bufbyte)) > 0) {

                        fos.write(bufbyte, 0, len);
                    }
                    fos.close();

                }
                zipEntry = zipInputStream.getNextEntry();
            }
        }

        catch (IOException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_EXTRACT_ZIP_CONTENTS);
        }
        try {
            zipInputStream.closeEntry();
            zipInputStream.close();
        } catch (IOException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return newFile;
    }


    private void logEvent(StandardBusinessDocumentWrapper inputDocument, ProcessState processState) {
            eventLog.log(new Event().setProcessStates(processState)
                    .setReceiver(inputDocument.getReceiverOrgNumber())
                    .setSender(inputDocument.getSenderOrgNumber()));
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
}
