package no.difi.meldingsutveksling.noarkexchange;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.adresseregmock.AdressRegisterFactory;
import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.eventlog.ProcessState;
import no.difi.meldingsutveksling.noark.NOARKSystem;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.Partner;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.PartnerIdentification;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.oxalisexchange.ByteArrayImpl;
import no.difi.meldingsutveksling.oxalisexchange.Kvittering;
import no.difi.meldingsutveksling.oxalisexchange.OxalisMessageReceiverTemplate;
import org.apache.commons.io.FileUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.BindingType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
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
public class KnutePunktReceiveImpl extends OxalisMessageReceiverTemplate implements SOAReceivePort  {
    private EventLog eventLog = EventLog.create();
    private static final String MIME_TYPE = "application/xml";
    private static final String WRITE_TO = System.getProperty("user.home") + File.separator +"testToRemove"+File.separator +"kvitteringSbd.xml";
    public CorrelationInformation receive(@WebParam(name = "StandardBusinessDocument", targetNamespace = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader", partName = "receiveResponse") StandardBusinessDocument receiveResponse)  {

      eventLogManager(receiveResponse,null,ProcessState.SBD_RECIEVED);
        try {
            forberedKvittering(receiveResponse, "leveringsKvittering");
        } catch (IOException e) {
            eventLogManager(receiveResponse, e, ProcessState.LEVERINGS_KVITTERING_SENT_FAILED);
        }

        String RSA_INSTANCE = "RSA";
        //*** query to Elma to get PK
        List<Partner> senders = receiveResponse.getStandardBusinessDocumentHeader().getSender();
        Partner sender = senders.get(0);
        PartnerIdentification orgNr = sender.getIdentifier();
        String[] orgNrArr = orgNr.getValue().split(":");

        //*** get payload *****
        Payload payload = (Payload) receiveResponse.getAny();
        String aesInRsa = payload.getEncryptionKey();
        String payloadString = payload.getAsice();
        byte[] aesInDisc = DatatypeConverter.parseBase64Binary(aesInRsa);
        byte[] aesEncZip = DatatypeConverter.parseBase64Binary(payloadString);
        eventLogManager(receiveResponse,null,ProcessState.DECRYPTION_SUCCESS);

        //*** get rsa cipher decrypt *****
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(RSA_INSTANCE);
        } catch (NoSuchAlgorithmException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);

        } catch (NoSuchPaddingException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);
        }
        PrivateKey privateKey = null;
        try {
            privateKey = loadPrivateKey();
        } catch (IOException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
        } catch (InvalidKeyException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);
        }
        byte[] aesKey = new byte[0];
        try {
            aesKey = cipher.doFinal(aesInDisc);
        } catch (IllegalBlockSizeException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);
        } catch (BadPaddingException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);
        }

        //*** get aes cipher decrypt *****
        Cipher aesCipher = null;
        try {
            aesCipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);
        } catch (NoSuchPaddingException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, "AES");
        SecureRandom secureRandom = null;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);
        }


        try {
            aesCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, secureRandom);

        } catch (InvalidKeyException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);
        }
        byte[] zipTobe = new byte[0];
        try {
            zipTobe = aesCipher.doFinal(aesEncZip);

        } catch (IllegalBlockSizeException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);
        } catch (BadPaddingException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);
        }  catch (GeneralSecurityException e) {
            eventLogManager(receiveResponse,e,ProcessState.DECRYPTION_ERROR);
        }
        eventLogManager(receiveResponse,null,ProcessState.DECRYPTION_SUCCESS);
        // Lage zip fil av byteArray
        File bestEdu = null;
        try {
            bestEdu = goGetBestEdu(zipTobe);
        } catch (IOException e) {
            eventLogManager(receiveResponse,e,ProcessState.SOME_OTHER_EXCEPTION);
        }

        // Ta ut "første entry" eller "entry basert på filnavn?", finne edu medling.

        // Best/EDU Melding er en PutMesssageRequestType - må gjøres om
        PutMessageRequestType mrt = new PutMessageRequestType();

        //*** Unmarshall xml*****
        PutMessageRequestType putMessageRequestType = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
            Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
            //putMessageRequestType = (PutMessageRequestType) unMarshaller.unmarshal(bestEdu);
        } catch (JAXBException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // Send the edu

        NOARKSystem noarkSystem = new NOARKSystem();
      //  noarkSystem.sendEduMeldig( putMessageRequestType);

        return new CorrelationInformation();
    }


    private void forberedKvittering(StandardBusinessDocument receiveResponse,String kvitteringsType) throws IOException {
        Dokumentpakker dokumentpakker= new Dokumentpakker();
        List<Partner> partnerList = receiveResponse.getStandardBusinessDocumentHeader().getSender();
        List<Partner> recieverList = receiveResponse.getStandardBusinessDocumentHeader().getReceiver();
        String sendTo= partnerList.get(0).getIdentifier().getValue().split(":")[1];
        String recievedBy = recieverList.get(0).getIdentifier().getValue().split(":")[1];
        String instanceIdentifier = receiveResponse.getStandardBusinessDocumentHeader().getBusinessScope().getScope().get(0).getInstanceIdentifier();
        Certificate certificate = (Certificate) AdressRegisterFactory.createAdressRegister().getCertificate(sendTo);
        Noekkelpar noekkelpar = new Noekkelpar(loadPrivateKey(),certificate);
        Avsender.Builder avsenderBuilder = Avsender.builder(new Organisasjonsnummer(recievedBy), noekkelpar);
        Avsender avsender = avsenderBuilder.build();
        Mottaker mottaker = new Mottaker(new Organisasjonsnummer(sendTo), AdressRegisterFactory.createAdressRegister().getPublicKey(sendTo));
        ByteArrayImpl byteArray = new ByteArrayImpl(genererKvittering(receiveResponse,kvitteringsType), kvitteringsType.concat(".xml"), MIME_TYPE);
        byte[] resultSbd = dokumentpakker.pakkDokumentISbd(byteArray, avsender, mottaker, instanceIdentifier);
        File file = new File(WRITE_TO);
        try {
            FileUtils.writeByteArrayToFile(file, resultSbd);
        } catch (IOException e) {
           eventLogManager(receiveResponse,e,ProcessState.SOME_OTHER_EXCEPTION);
        }
    }

    private byte[] genererKvittering(StandardBusinessDocument receiveResponse, String kvitteringsType) {
        Kvittering kvittering = new Kvittering(kvitteringsType);
        XStream xStream = new XStream();
        String kvitteringXml = xStream.toXML(kvittering);
        return kvitteringXml.getBytes();
    }

    private File goGetBestEdu(byte[] bytes) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes));
        ZipEntry zipEntry = null;
        String outputFolder = System.getProperty("user.home") + File.separator + "testToRemove" +
                File.separator + "Zip Output";
        File newFile = null;
        try {
            zipEntry = zipInputStream.getNextEntry();
        } catch (IOException e) {
            e.printStackTrace();
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
                    e.printStackTrace();
                }
                byte[] bufbyte = new byte[1024];
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

    private void eventLogManager(StandardBusinessDocument receiveResponse, Throwable e,ProcessState processState) {
        if (null != e)
            eventLog.log(new Event().setProcessStates(processState).setExceptionMessage(e.toString())
                    .setReceiver( receiveResponse.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue().split(":")[1])
                    .setSender(receiveResponse.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue().split(":")[1]));
        else
            eventLog.log(new Event().setProcessStates(processState)
                    .setReceiver( receiveResponse.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue().split(":")[1])
                    .setSender(receiveResponse.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue().split(":")[1]));
    }



}
