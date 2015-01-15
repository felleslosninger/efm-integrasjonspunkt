package no.difi.meldingsutveksling.noarkexchange;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.adresseregister.client.AdresseRegisterClient;
import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.dokumentpakking.kvit.ObjectFactory;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.dokumentpakking.service.KvitteringType;
import no.difi.meldingsutveksling.dokumentpakking.service.SignAFile;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.*;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noark.NOARKSystem;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.*;
import no.difi.meldingsutveksling.oxalisexchange.ByteArrayImpl;
import no.difi.meldingsutveksling.oxalisexchange.Kvittering;
import no.difi.meldingsutveksling.oxalisexchange.OxalisMessageReceiverTemplate;
import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.io.*;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: glennbech
 * Date: 25.11.14
 * Time: 12:43
 * To change this template use File | Settings | File Templates.
 */
@WebService(portName = "ReceivePort", serviceName = "receive", targetNamespace = "", wsdlLocation = "file:/Users/glennbech/dev/meldingsutvikling-mellom-offentlige-virksomheter/praktiskprove/knutepunkt/src/main/webapp/WEB-INF/wsdl/knutepunktReceive.wsdl", endpointInterface = "no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class KnutePunktReceiveImpl extends OxalisMessageReceiverTemplate implements SOAReceivePort {

    private static final String KVITTERING = "Kvittering";
    private static final String BEST_EDU = "BEST_EDU";
    private static final String KVITTERING_CONSTANT="kvittering";
    private static final int MAGIC_NR = 1024;
    private EventLog eventLog = EventLog.create();
    private static final String MIME_TYPE = "application/xml";
    private static final String WRITE_TO = System.getProperty("user.home") + File.separator + "testToRemove" + File.separator + "kvitteringSbd.xml";


    private SendMessageTemplateImpl sendMessageTemplate ;

    @Resource
    private WebServiceContext context;

    private NOARKSystem noarkSystem;

    private AdresseRegisterClient adresseRegisterClient;

    public CorrelationInformation receive(@WebParam(name = "StandardBusinessDocument", targetNamespace = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader", partName = "receiveResponse") StandardBusinessDocument receiveResponse){
        ServletContext servletContext =
                    (ServletContext) context.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        noarkSystem = ctx.getBean(NOARKSystem.class);
        adresseRegisterClient = ctx.getBean(AdresseRegisterClient.class);

        Organisasjonsnummer sender;
        Organisasjonsnummer reciever;

        String convId;
        Avsender avsender;
        SignAFile signAFile;
        if (isReciept(receiveResponse.getStandardBusinessDocumentHeader())) {
            eventLogManager(receiveResponse, null, ProcessState.KVITTERING_MOTTATT);
        } else {
            eventLogManager(receiveResponse, null, ProcessState.SBD_RECIEVED);

            try {
                  forberedKvittering(receiveResponse, "leveringsKvittering");
                  sender = new Organisasjonsnummer( receiveResponse.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue().split(":")[1]);
                  reciever= new Organisasjonsnummer( receiveResponse.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue().split(":")[1]);
                  convId = receiveResponse.getStandardBusinessDocumentHeader().getBusinessScope().getScope().get(0).getInstanceIdentifier();
                  Noekkelpar noekkelpar = new Noekkelpar(loadPrivateKey(),  adresseRegisterClient.getCertificate(reciever.toString()));
                  avsender= new Avsender(reciever,noekkelpar);
                  signAFile = new SignAFile();

                //todo hvordan endrer vi implementasjon, se på jspf
                new OxalisSendMessageTemplate().sendSBD(new CreateSBD().createSBD(reciever,sender, new ObjectFactory().createKvittering(signAFile.signIt(receiveResponse.getAny(),avsender, KvitteringType.LEVERING)),convId,KVITTERING_CONSTANT));
                eventLogManager(receiveResponse,null, ProcessState.LEVERINGS_KVITTERING_SENT);
            } catch (IOException e) {
                eventLogManager(receiveResponse, e, ProcessState.LEVERINGS_KVITTERING_SENT_FAILED);
                throw new MeldingsUtvekslingRuntimeException(e);
            }

            String RSA_INSTANCE = "RSA";

            Payload payload = null;

            JAXBContext jaxbContextP = null;
            try {
                jaxbContextP = JAXBContext.newInstance(Payload.class);
            } catch (JAXBException e) {
                eventLogManager(receiveResponse, e, ProcessState.SOME_OTHER_EXCEPTION);
                throw new MeldingsUtvekslingRuntimeException(e);
            }
            Unmarshaller unMarshallerP = null;
            try {
                unMarshallerP = jaxbContextP.createUnmarshaller();
            } catch (JAXBException e) {
                eventLogManager(receiveResponse, e, ProcessState.DECRYPTION_ERROR);
                throw new MeldingsUtvekslingRuntimeException(e);
            }

            try {
                payload = unMarshallerP.unmarshal((org.w3c.dom.Node) receiveResponse.getAny(), Payload.class).getValue();
            } catch (JAXBException e) {
                eventLogManager(receiveResponse, e, ProcessState.DECRYPTION_ERROR);
                throw new MeldingsUtvekslingRuntimeException(e);
            }

            byte[] cmsEncZip = DatatypeConverter.parseBase64Binary(payload.getContent());


            PrivateKey privateKey = null;
            try {
                privateKey = loadPrivateKey();
            } catch (IOException e) {
                eventLogManager(receiveResponse, e, ProcessState.DECRYPTION_ERROR);
                throw new MeldingsUtvekslingRuntimeException(e);
            }

            CmsUtil cmsUtil = new CmsUtil();
           byte[] zipTobe= cmsUtil.decryptCMS(cmsEncZip,privateKey);
            eventLogManager(receiveResponse, null, ProcessState.DECRYPTION_SUCCESS);
            // Lage zip fil av byteArray
            File bestEdu;
            try {

                bestEdu = goGetBestEdu(receiveResponse, zipTobe);
                eventLogManager(receiveResponse, null, ProcessState.BESTEDU_EXTRACTED);
            } catch (IOException e) {
                eventLogManager(receiveResponse, e, ProcessState.SOME_OTHER_EXCEPTION);
                throw new MeldingsUtvekslingRuntimeException(e);
            }


            //*** Unmarshall xml*****
            PutMessageRequestType putMessageRequestType;
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
                Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
                putMessageRequestType = unMarshaller.unmarshal(new StreamSource(bestEdu), PutMessageRequestType.class).getValue();
            } catch (JAXBException e) {
                eventLogManager(receiveResponse, e, ProcessState.SOME_OTHER_EXCEPTION);
                throw new IllegalStateException(e.getMessage(), e);
            }

            PutMessageResponseType response = noarkSystem.sendEduMeldig(putMessageRequestType);
            if (null != response) {
                AppReceiptType result = response.getResult();
                if (null == result) {
                    eventLogManager(receiveResponse, null, ProcessState.ARKIVE_RESPONSE_NULL);
                } else {
                    eventLogManager(receiveResponse, null, ProcessState.BEST_EDU_SENT);

                    try {
                        new OxalisSendMessageTemplate().sendSBD(new CreateSBD().createSBD(sender,reciever,signAFile.signIt(receiveResponse.getAny(),avsender, KvitteringType.AAPNING),convId,KVITTERING_CONSTANT));
                    } catch (IOException e) {
                        throw new MeldingsUtvekslingRuntimeException(e);
                    }

                }
            } else {
                eventLogManager(receiveResponse, null, ProcessState.NO_ARKIVE_UNAVAILABLE);
            }
        }
        return new CorrelationInformation();
    }

    public AdresseRegisterClient getAdresseRegisterClient() {
        return adresseRegisterClient;
    }

    public void setAdresseRegisterClient(AdresseRegisterClient adresseRegisterClient) {
        this.adresseRegisterClient = adresseRegisterClient;
    }

    private boolean isReciept(StandardBusinessDocumentHeader standardBusinessDocumentHeader) {
        return standardBusinessDocumentHeader.getDocumentIdentification().getType().equalsIgnoreCase(KVITTERING_CONSTANT);
    }


    private void forberedKvittering(StandardBusinessDocument receiveResponse, String kvitteringsType) throws IOException {
        Dokumentpakker dokumentpakker = new Dokumentpakker();
        List<Partner> partnerList = receiveResponse.getStandardBusinessDocumentHeader().getSender();
        List<Partner> recieverList = receiveResponse.getStandardBusinessDocumentHeader().getReceiver();
        String sendTo = partnerList.get(0).getIdentifier().getValue().split(":")[1];
        String recievedBy = recieverList.get(0).getIdentifier().getValue().split(":")[1];
        String instanceIdentifier = receiveResponse.getStandardBusinessDocumentHeader().getBusinessScope().getScope().get(0).getInstanceIdentifier();
        if (instanceIdentifier.contains(BEST_EDU)) {
            instanceIdentifier.replace(BEST_EDU, "Kvittering");
        }

        Certificate certificate =adresseRegisterClient.getCertificate(recievedBy);
        Noekkelpar noekkelpar = new Noekkelpar(loadPrivateKey(), certificate);
        Avsender.Builder avsenderBuilder = Avsender.builder(new Organisasjonsnummer(recievedBy), noekkelpar);
        Avsender avsender = avsenderBuilder.build();
        Mottaker mottaker = new Mottaker(new Organisasjonsnummer(sendTo),(X509Certificate) certificate);
        ByteArrayImpl byteArray = new ByteArrayImpl(genererKvittering( kvitteringsType), kvitteringsType.concat(".xml"), MIME_TYPE);
        byte[] resultSbd = dokumentpakker.pakkDokumentISbd(byteArray, avsender, mottaker, instanceIdentifier, KVITTERING);
        File file = new File(WRITE_TO);
        try {
            FileUtils.writeByteArrayToFile(file, resultSbd);
        } catch (IOException e) {
            eventLogManager(receiveResponse, e, ProcessState.SOME_OTHER_EXCEPTION);
        }
    }

    private byte[] genererKvittering(  String kvitteringsType) {
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
            eventLogManager(sbd, e, ProcessState.SOME_OTHER_EXCEPTION);
        }
        while (null != zipEntry) {
            String fileName = zipEntry.getName();
            if ("edu_best.xml".equals(fileName)) {

                newFile = new File(outputFolder + File.separator + fileName);
                FileOutputStream fos = null;
                new File(newFile.getParent()).mkdirs();
                try {
                    fos = new FileOutputStream(newFile);
                } catch (FileNotFoundException e) {
                    eventLogManager(sbd, e, ProcessState.SOME_OTHER_EXCEPTION);
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

    private void eventLogManager(StandardBusinessDocument receiveResponse, Throwable e, ProcessState processState) {
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

    public NOARKSystem getNoarkSystem() {
        return noarkSystem;
    }

    public void setNoarkSystem(NOARKSystem noarkSystem) {
        this.noarkSystem = noarkSystem;
    }


}
