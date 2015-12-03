package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.oxalisexchange.OxalisMessageReceiverTemplate;
import no.difi.meldingsutveksling.services.AdresseregisterService;
import no.difi.meldingsutveksling.services.CertificateException;
import no.difi.meldingsutveksling.transport.TransportFactory;
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

/**
 *
 */

@Component("recieveService")
@WebService(portName = "ReceivePort", serviceName = "receive", targetNamespace = "", endpointInterface = "no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class IntegrajonspunktReceiveImpl extends OxalisMessageReceiverTemplate implements SOAReceivePort {

    private static final String KVITTERING = "Kvittering";
    private static final String BEST_EDU = "BEST_EDU";
    private static final String KVITTERING_CONSTANT = "kvittering";
    private static final int MAGIC_NR = 1024;
    public static final String SBD_NAMESPACE = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader";
    private EventLog eventLog = EventLog.create();
    private static final String MIME_TYPE = "application/xml";
    private static final String WRITE_TO = System.getProperty("user.home") + File.separator + "testToRemove" + File.separator + "kvitteringSbd.xml";

    @Autowired
    TransportFactory transportFactory;

    @Autowired
    private NoarkClient localNoark;

    @Autowired
    private AdresseregisterService adresseRegisterClient;

    @Autowired
    private IntegrasjonspunktConfig config;


    @Autowired
    private IntegrasjonspunktNokkel keyInfo;


    public IntegrajonspunktReceiveImpl() {
    }

    public CorrelationInformation receive(@WebParam(name = "StandardBusinessDocument", targetNamespace = SBD_NAMESPACE, partName = "receiveResponse") StandardBusinessDocument standardBusinessDocument) {
        return forwardToNoarkSystem(standardBusinessDocument);
    }

    public CorrelationInformation forwardToNoarkSystem(StandardBusinessDocument standardBusinessDocument) {
        StandardBusinessDocumentWrapper inputDocument = new StandardBusinessDocumentWrapper(standardBusinessDocument);
        if (!verifyCertificatesForSenderAndReceiver(inputDocument.getReceiverOrgNumber(), inputDocument.getSenderOrgNumber())) {
            throw new MeldingsUtvekslingRuntimeException("invalid certificate for sender or recipient (" + inputDocument.getSenderOrgNumber() + "," + inputDocument.getReceiverOrgNumber());
        }

        if (isReciept(standardBusinessDocument.getStandardBusinessDocumentHeader())) {
            logEvent(inputDocument, ProcessState.KVITTERING_MOTTATT);
            return new CorrelationInformation();
        }


        logEvent(inputDocument, ProcessState.SBD_RECIEVED);

        Payload payload = inputDocument.getPayload();

        byte[] decryptedPayload = decrypt(payload);
        logEvent(inputDocument, null, ProcessState.DECRYPTION_SUCCESS);
        File decompressedPayload;
        try {
            decompressedPayload = decompressToFile(inputDocument, decryptedPayload);
            logEvent(inputDocument, null, ProcessState.BESTEDU_EXTRACTED);
        } catch (IOException e) {
            logEvent(inputDocument, e, ProcessState.SOME_OTHER_EXCEPTION);
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        PutMessageRequestType putMessageRequestType = extractBestEdu(standardBusinessDocument, decompressedPayload);
        forwardToNoarkSystemAndSendReceipt(inputDocument, putMessageRequestType);

        return new CorrelationInformation();
    }

    private byte[] decrypt(Payload payload) {
        byte[] cmsEncZip = DatatypeConverter.parseBase64Binary(payload.getContent());
        CmsUtil cmsUtil = new CmsUtil();
        return cmsUtil.decryptCMS(cmsEncZip, keyInfo.loadPrivateKey());
    }

    private boolean verifyCertificatesForSenderAndReceiver(String orgNumberReceiver, String orgNumberSender) {
        boolean validCertificates;
        try {
            adresseRegisterClient.getCertificate(orgNumberReceiver);
            adresseRegisterClient.getCertificate(orgNumberSender);
            validCertificates = true;
        } catch (CertificateException e) {
            validCertificates = false;
        }
        return validCertificates;
    }

    private PutMessageRequestType extractBestEdu(StandardBusinessDocument standardBusinessDocument, File bestEdu) {
        PutMessageRequestType putMessageRequestType;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
            Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
            putMessageRequestType = unMarshaller.unmarshal(new StreamSource(bestEdu), PutMessageRequestType.class).getValue();
        } catch (JAXBException e) {
            StandardBusinessDocumentWrapper inputDocument = new StandardBusinessDocumentWrapper(standardBusinessDocument);
            logEvent(inputDocument, e, ProcessState.SOME_OTHER_EXCEPTION);
            throw new IllegalStateException(e.getMessage(), e);
        }
        return putMessageRequestType;
    }

    private void forwardToNoarkSystemAndSendReceipt(StandardBusinessDocumentWrapper inputDocument, PutMessageRequestType putMessageRequestType) {
        PutMessageResponseType response = localNoark.sendEduMelding(putMessageRequestType);
        if (response != null) {
            AppReceiptType result = response.getResult();
            if (null == result) {
                logEvent(inputDocument, null, ProcessState.ARCHIVE_NULL_RESPONSE);
            } else {
                logEvent(inputDocument, null, ProcessState.BEST_EDU_SENT);
            }
        } else {
            logEvent(inputDocument, null, ProcessState.ARCHIVE_NOT_AVAILABLE);
        }
    }

    private boolean isReciept(StandardBusinessDocumentHeader standardBusinessDocumentHeader) {
        return standardBusinessDocumentHeader.getDocumentIdentification().getType().equalsIgnoreCase(KVITTERING_CONSTANT);
    }

    private File decompressToFile(StandardBusinessDocumentWrapper inputDocument, byte[] bytes) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes));
        ZipEntry zipEntry = null;
        String outputFolder = System.getProperty("user.home") + File.separator + "testToRemove" +
                File.separator + "Zip Output";
        File newFile = null;
        try {
            zipEntry = zipInputStream.getNextEntry();
        } catch (IOException e) {
            logEvent(inputDocument, e, ProcessState.SOME_OTHER_EXCEPTION);
        }
        while (null != zipEntry) {
            String fileName = zipEntry.getName();
            if ("edu_test.xml".equals(fileName)) {

                newFile = new File(outputFolder + File.separator + fileName);
                FileOutputStream fos = null;
                new File(newFile.getParent()).mkdirs();
                try {
                    fos = new FileOutputStream(newFile);
                } catch (FileNotFoundException e) {
                    logEvent(inputDocument, e, ProcessState.SOME_OTHER_EXCEPTION);
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
        zipInputStream.closeEntry();
        zipInputStream.close();
        return newFile;
    }


    private void logEvent(StandardBusinessDocumentWrapper inputDocument, ProcessState sbdRecieved) {
        logEvent(inputDocument, null, sbdRecieved);
    }

    private void logEvent(StandardBusinessDocumentWrapper inputDocument, Throwable e, ProcessState processState) {
        if (null != e) {
            eventLog.log(new Event().setProcessStates(processState).setExceptionMessage(e.toString())
                    .setReceiver(inputDocument.getReceiverOrgNumber())
                    .setSender(inputDocument.getSenderOrgNumber()));
        } else {
            eventLog.log(new Event().setProcessStates(processState)
                    .setReceiver(inputDocument.getReceiverOrgNumber())
                    .setSender(inputDocument.getSenderOrgNumber()));
        }
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

    public IntegrasjonspunktConfig getConfig() {
        return config;
    }

    public void setConfig(IntegrasjonspunktConfig config) {
        this.config = config;
    }

    public IntegrasjonspunktNokkel getKeyInfo() {
        return keyInfo;
    }

    public void setKeyInfo(IntegrasjonspunktNokkel keyInfo) {
        this.keyInfo = keyInfo;
    }
}
