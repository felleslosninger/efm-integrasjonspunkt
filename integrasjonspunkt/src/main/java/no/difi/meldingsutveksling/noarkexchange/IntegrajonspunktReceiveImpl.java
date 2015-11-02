package no.difi.meldingsutveksling.noarkexchange;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.CertificateValidator;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.dokumentpakking.service.KvitteringType;
import no.difi.meldingsutveksling.dokumentpakking.service.SignAFile;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.ProcessState;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.Partner;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.oxalisexchange.ByteArrayImpl;
import no.difi.meldingsutveksling.oxalisexchange.Kvittering;
import no.difi.meldingsutveksling.oxalisexchange.OxalisMessageReceiverTemplate;
import no.difi.meldingsutveksling.services.AdresseregisterService;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
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
    private EventLog eventLog = EventLog.create();
    private static final String MIME_TYPE = "application/xml";
    private static final String WRITE_TO = System.getProperty("user.home") + File.separator + "testToRemove" + File.separator + "kvitteringSbd.xml";

    @Autowired
    @Qualifier(value = "multiTransport")
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

    public CorrelationInformation receive(@WebParam(name = "StandardBusinessDocument", targetNamespace = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader", partName = "receiveResponse") StandardBusinessDocument receiveResponse) {

        if (isReciept(receiveResponse.getStandardBusinessDocumentHeader())) {
            logEvent(receiveResponse, ProcessState.KVITTERING_MOTTATT);
            return new CorrelationInformation();
        }

        String orgNumberSender = receiveResponse.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue().split(":")[1];
        Organisasjonsnummer sender = new Organisasjonsnummer(orgNumberSender);
        String orgNumberReceiver = receiveResponse.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue().split(":")[1];
        Organisasjonsnummer reciever = new Organisasjonsnummer(orgNumberReceiver);

        verifyCertificatesForSenderAndReceiver(orgNumberReceiver, orgNumberSender);


        logEvent(receiveResponse, ProcessState.SBD_RECIEVED);

        forberedKvittering(receiveResponse, "leveringsKvittering");

        String convId = receiveResponse.getStandardBusinessDocumentHeader().getBusinessScope().getScope().get(0).getInstanceIdentifier();
        Noekkelpar noekkelpar = new Noekkelpar(keyInfo.loadPrivateKey(), adresseRegisterClient.getCertificate(reciever.toString()));
        Avsender avsender = new Avsender(reciever, noekkelpar);
        SignAFile signAFile = new SignAFile();

        JAXBContext jaxbContextP;
        Unmarshaller unMarshallerP;

        Payload payload;
        try {
            jaxbContextP = JAXBContext.newInstance(Payload.class);
            unMarshallerP = jaxbContextP.createUnmarshaller();
            payload = unMarshallerP.unmarshal((org.w3c.dom.Node) receiveResponse.getAny(), Payload.class).getValue();
        } catch (JAXBException e) {
            logEvent(receiveResponse, e, ProcessState.SOME_OTHER_EXCEPTION);
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        byte[] cmsEncZip = DatatypeConverter.parseBase64Binary(payload.getContent());
        CmsUtil cmsUtil = new CmsUtil();
        byte[] zipTobe = cmsUtil.decryptCMS(cmsEncZip, keyInfo.loadPrivateKey());
        logEvent(receiveResponse, null, ProcessState.DECRYPTION_SUCCESS);
        File bestEdu;
        try {
            bestEdu = goGetBestEdu(receiveResponse, zipTobe);
            logEvent(receiveResponse, null, ProcessState.BESTEDU_EXTRACTED);
        } catch (IOException e) {
            logEvent(receiveResponse, e, ProcessState.SOME_OTHER_EXCEPTION);
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        PutMessageRequestType putMessageRequestType = extractBestEdu(receiveResponse, bestEdu);
        forwardToNoarkSystemAndSendReceipt(receiveResponse, sender, reciever, convId, avsender, signAFile, putMessageRequestType);
        return new CorrelationInformation();
    }

    public CorrelationInformation forwardToNoarkSystem(StandardBusinessDocument standardBusinessDocument) {        //storeToFile(((Payload) standardBusinessDocument
        if (isReciept(standardBusinessDocument.getStandardBusinessDocumentHeader())) {
            logEvent(standardBusinessDocument, ProcessState.KVITTERING_MOTTATT);
            return new CorrelationInformation();
        }

        String orgNumberSender = standardBusinessDocument.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue().split(":")[1];
        Organisasjonsnummer sender = new Organisasjonsnummer(orgNumberSender);
        String orgNumberReceiver = standardBusinessDocument.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue().split(":")[1];
        Organisasjonsnummer reciever = new Organisasjonsnummer(orgNumberReceiver);

        verifyCertificatesForSenderAndReceiver(orgNumberReceiver, orgNumberSender);


        logEvent(standardBusinessDocument, ProcessState.SBD_RECIEVED);

        forberedKvittering(standardBusinessDocument, "leveringsKvittering");

        String convId = standardBusinessDocument.getStandardBusinessDocumentHeader().getBusinessScope().getScope().get(0).getInstanceIdentifier();
        Noekkelpar noekkelpar = new Noekkelpar(keyInfo.loadPrivateKey(), adresseRegisterClient.getCertificate(reciever.toString()));
        Avsender avsender = new Avsender(reciever, noekkelpar);
        SignAFile signAFile = new SignAFile();

        Payload payload = (Payload) standardBusinessDocument.getAny();

        byte[] cmsEncZip = DatatypeConverter.parseBase64Binary(payload.getContent());
        CmsUtil cmsUtil = new CmsUtil();
        byte[] zipTobe = cmsUtil.decryptCMS(cmsEncZip, keyInfo.loadPrivateKey());
        logEvent(standardBusinessDocument, null, ProcessState.DECRYPTION_SUCCESS);
        File bestEdu;
        try {
            bestEdu = goGetBestEdu(standardBusinessDocument, zipTobe);
            logEvent(standardBusinessDocument, null, ProcessState.BESTEDU_EXTRACTED);
        } catch (IOException e) {
            logEvent(standardBusinessDocument, e, ProcessState.SOME_OTHER_EXCEPTION);
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        PutMessageRequestType putMessageRequestType = extractBestEdu(standardBusinessDocument, bestEdu);
        forwardToNoarkSystemAndSendReceipt(standardBusinessDocument, sender, reciever, convId, avsender, signAFile, putMessageRequestType);

        return new CorrelationInformation();
    }

    private void verifyCertificatesForSenderAndReceiver(String orgNumberReceiver, String orgNumberSender) {

        CertificateValidator validator = new CertificateValidator();
        validator.validate((X509Certificate) adresseRegisterClient.getCertificate(orgNumberReceiver));
        validator.validate((X509Certificate) adresseRegisterClient.getCertificate(orgNumberSender));
    }

    private PutMessageRequestType extractBestEdu(StandardBusinessDocument receiveResponse, File bestEdu) {
        PutMessageRequestType putMessageRequestType;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
            Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
            putMessageRequestType = unMarshaller.unmarshal(new StreamSource(bestEdu), PutMessageRequestType.class).getValue();
        } catch (JAXBException e) {
            logEvent(receiveResponse, e, ProcessState.SOME_OTHER_EXCEPTION);
            throw new IllegalStateException(e.getMessage(), e);
        }
        return putMessageRequestType;
    }

    private void forwardToNoarkSystemAndSendReceipt(StandardBusinessDocument receiveResponse, Organisasjonsnummer sender, Organisasjonsnummer reciever, String convId, Avsender avsender, SignAFile signAFile, PutMessageRequestType putMessageRequestType) {
        PutMessageResponseType response = localNoark.sendEduMelding(putMessageRequestType);
        if (response != null) {
            AppReceiptType result = response.getResult();
            if (null == result) {
                logEvent(receiveResponse, null, ProcessState.ARCHIVE_NULL_RESPONSE);
            } else {
                logEvent(receiveResponse, null, ProcessState.BEST_EDU_SENT);
                Document receipt = new CreateSBD().createSBD(sender, reciever, signAFile.signIt(receiveResponse.getAny(), avsender, KvitteringType.AAPNING), convId, KVITTERING_CONSTANT);
                Transport transport = transportFactory.createTransport(receipt);
                transport.send(config.getConfiguration(), receipt);
            }
        } else {
            logEvent(receiveResponse, null, ProcessState.ARCHIVE_NOT_AVAILABLE);
        }
    }

    private boolean isReciept(StandardBusinessDocumentHeader standardBusinessDocumentHeader) {
        return standardBusinessDocumentHeader.getDocumentIdentification().getType().equalsIgnoreCase(KVITTERING_CONSTANT);
    }

    private void forberedKvittering(StandardBusinessDocument receiveResponse, String kvitteringsType) {
        Dokumentpakker dokumentpakker = new Dokumentpakker(keyInfo.getSignatureHelper());
        List<Partner> partnerList = receiveResponse.getStandardBusinessDocumentHeader().getSender();
        List<Partner> recieverList = receiveResponse.getStandardBusinessDocumentHeader().getReceiver();
        String sendTo = partnerList.get(0).getIdentifier().getValue().split(":")[1];
        String recievedBy = recieverList.get(0).getIdentifier().getValue().split(":")[1];
        String instanceIdentifier = receiveResponse.getStandardBusinessDocumentHeader().getBusinessScope().getScope().get(0).getInstanceIdentifier();
        if (instanceIdentifier.contains(BEST_EDU)) {
            instanceIdentifier = instanceIdentifier.replace(BEST_EDU, "Kvittering");
        }

        Certificate certificate = adresseRegisterClient.getCertificate(recievedBy);
        Noekkelpar noekkelpar = new Noekkelpar(keyInfo.loadPrivateKey(), certificate);
        Avsender.Builder avsenderBuilder = Avsender.builder(new Organisasjonsnummer(recievedBy), noekkelpar);
        Avsender avsender = avsenderBuilder.build();
        Mottaker mottaker = new Mottaker(new Organisasjonsnummer(sendTo), (X509Certificate) certificate);
        try {
            ByteArrayImpl byteArray = new ByteArrayImpl(genererKvittering(kvitteringsType), kvitteringsType.concat(".xml"), MIME_TYPE);
            byte[] resultSbd = dokumentpakker.pakkTilByteArray(byteArray, avsender, mottaker, instanceIdentifier, KVITTERING);
            File file = new File(WRITE_TO);
            FileUtils.writeByteArrayToFile(file, resultSbd);
        } catch (IOException e) {
            logEvent(receiveResponse, e, ProcessState.SOME_OTHER_EXCEPTION);
        }
    }

    private byte[] genererKvittering(String kvitteringsType) {
        Kvittering kvittering = new Kvittering(kvitteringsType);
        XStream xStream = new XStream();
        String kvitteringXml = xStream.toXML(kvittering);
        return kvitteringXml.getBytes();
    }

    private File goGetBestEdu(StandardBusinessDocument sbd, byte[] bytes) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes));
        ZipEntry zipEntry = null;
        String outputFolder = System.getProperty("user.home") + File.separator + "testToRemove" +
                File.separator + "Zip Output";
        File newFile = null;
        try {
            zipEntry = zipInputStream.getNextEntry();
        } catch (IOException e) {
            logEvent(sbd, e, ProcessState.SOME_OTHER_EXCEPTION);
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
                    logEvent(sbd, e, ProcessState.SOME_OTHER_EXCEPTION);
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


    private void logEvent(StandardBusinessDocument receiveResponse, ProcessState sbdRecieved) {
        logEvent(receiveResponse, null, sbdRecieved);
    }

    private void logEvent(StandardBusinessDocument receiveResponse, Throwable e, ProcessState processState) {
        if (null != e) {
            eventLog.log(new Event().setProcessStates(processState).setExceptionMessage(e.toString())
                    .setReceiver(receiveResponse.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue().split(":")[1])
                    .setSender(receiveResponse.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue().split(":")[1]));
        } else {
            eventLog.log(new Event().setProcessStates(processState)
                    .setReceiver(receiveResponse.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue().split(":")[1])
                    .setSender(receiveResponse.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue().split(":")[1]));
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
