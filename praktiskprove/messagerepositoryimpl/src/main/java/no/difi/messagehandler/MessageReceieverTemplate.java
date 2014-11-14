package no.difi.messagehandler;

import eu.peppol.PeppolMessageMetaData;
import no.difi.meldingsutveksling.domain.BestEduMessage;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.eventlog.ProcessState;
import no.difi.messagehandler.peppolmessageutils.MessageId;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

public abstract class MessageReceieverTemplate {

    private static final String PRIVATE_KEY_FILE = "958935429-oslo-kommune.pkcs8";
    private static final String PAYLOAD_ZIP =System.getProperty("user.home")+File.separator + "payload.zip";
    private EventLog eventLog = EventLog.create();

    abstract void sendLeveringskvittering();

    abstract void sendApningskvittering();

    public void receive(PeppolMessageMetaData metaData, Document document) throws  GeneralSecurityException, IOException {

        Map documentElements = null;
        try {
            documentElements = documentMapping(document);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        Node n = (Node) documentElements.get("DocumentIdentification");

        eventLog.log(new Event().setProcessStates(ProcessState.MESSAGE_RECIEVED).setTimeStamp(getTimeStamp()));

        if (isSBD(n)) {
            eventLog.log(new Event().setProcessStates(ProcessState.SBD_RECIEVED).setTimeStamp(getTimeStamp()));
            sendLeveringskvittering();
            eventLog.log(new Event().setProcessStates((ProcessState.LEVERINGS_KVITTERING_SENT)).setTimeStamp(getTimeStamp()));

            // get payloaed and encryption key
            Node  payload=(Node) documentElements.get("payload");


            // dekryptert payload (AES)
            byte[] asicFileBytes = getZipBytesFromDocument(payload);
            eventLog.log(new Event().setProcessStates(ProcessState.DECRYPTION_SUCCESS).setTimeStamp(getTimeStamp()));

            // Signaturvalidering
            ZipFile asicFile=verifySignature(asicFileBytes,payload);
            eventLog.log(new Event().setProcessStates(ProcessState.SIGNATURE_VALIDATED).setTimeStamp(getTimeStamp()));

            BestEduMessage bestEduMessage = getBestEduFromAsic(asicFile);
            senToNoark(bestEduMessage);
            eventLog.log(new Event().setProcessStates(ProcessState.BEST_EDU_SENT).setTimeStamp(getTimeStamp()));

            sendApningskvittering();
            eventLog.log(new Event().setProcessStates(ProcessState.AAPNINGS_KVITTERING_SENT).setTimeStamp(getTimeStamp()));

        } else {
            // BestEdu recieved
            MessageId messageId = new MessageId("");
            eventLog.log(new Event().setProcessStates(ProcessState.BEST_EDU_RECIEVED).setUuid(messageId.getUuid()).setTimeStamp(getTimeStamp()));
        }
    }

    private Timestamp getTimeStamp() {
        long time = System.currentTimeMillis();
        return    new java.sql.Timestamp(time);
    }

    /**
     * Maps out document elements as nodes
     *
     * @param document sbd or best/edu
     * @return List of node extended objects
     * @throws JAXBException
     */
    private Map<String, ? extends Node> documentMapping(Document document) throws JAXBException {

        Map list = new HashMap();
        NodeList sbdhNodes = document.getElementsByTagName("ns2:StandardBusinessDocumentHeader");
        Node sbdhElement = sbdhNodes.item(0);
        NodeList payloadNodes = document.getElementsByTagName("payload");
        NodeList childs = sbdhElement.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node n = childs.item(i);
            String name = n.getNodeName();
            if (name.contains("ns2:"))
                name = name.replace("ns2:", "");
            list.put(name, n);
        }
        for (int i = 0; i < payloadNodes.getLength(); i++) {
            Node n = payloadNodes.item(i);
            list.put("payload", n);
        }

        return list;
    }


    protected void senToNoark(BestEduMessage bestEduMessage) {
    }

    private BestEduMessage getBestEduFromAsic(ZipFile asicFile) {
        return null;
    }

    private ZipFile verifySignature(byte[] aesKey,Node payload) {
        String payloadTextContent = payload.getTextContent();
        byte[] aesEncZip = DatatypeConverter.parseBase64Binary(payloadTextContent);
        ZipFile zipFile=null;
        //*** get aes cipher decrypt *****
        Cipher aesCipher = null;
        try {
            aesCipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, "AES");
        SecureRandom secureRandom = null;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        try {
            aesCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, secureRandom);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        byte[] zipTobe = new byte[0];
        try {
            zipTobe = aesCipher.doFinal(aesEncZip);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

        File file = new File(PAYLOAD_ZIP);
        file.setWritable(true, false);
        try {
            FileUtils.writeByteArrayToFile(file, zipTobe);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
             zipFile = new ZipFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return zipFile;
    }

    private byte[] getZipBytesFromDocument(Node payload) throws GeneralSecurityException, IOException {
        String payloadTextContent = payload.getTextContent();
        NamedNodeMap namedNodeMap= payload.getAttributes();
        String aesInRsa = namedNodeMap.getNamedItem("encryptionKey").getTextContent();
        byte[] aesInDisc = DatatypeConverter.parseBase64Binary(aesInRsa);
        byte[] aesEncZip = DatatypeConverter.parseBase64Binary(payloadTextContent);

        //*** get rsa cipher decrypt *****
        Cipher cipher = Cipher.getInstance("RSA");
        PrivateKey privateKey = loadPrivateKey();
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] aesKey = cipher.doFinal(aesInDisc);

        return aesKey;
    }
    private boolean isSBD(Node node) {
        return node.getTextContent().contains("Sbd");
    }

    /**
     * Loads the private key from a pkcs8 file
     *
     * @return an private key
     * @throws java.io.IOException
     * @throws GeneralSecurityException
     */
    public PrivateKey loadPrivateKey()
            throws IOException, GeneralSecurityException {
        PrivateKey key = null;
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(PRIVATE_KEY_FILE);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            boolean inKey = false;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!inKey) {
                    if (line.startsWith("-----BEGIN ") &&
                            line.endsWith(" PRIVATE KEY-----")) {
                        inKey = true;
                    }
                    continue;
                } else {
                    if (line.startsWith("-----END ") &&
                            line.endsWith(" PRIVATE KEY-----")) {
                        inKey = false;
                        break;
                    }
                    builder.append(line);
                }
            }

            byte[] encoded = DatatypeConverter.parseBase64Binary(builder.toString());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            key = kf.generatePrivate(keySpec);

        } finally {
            closeSilent(is);
        }
        return key;
    }
    public static void closeSilent(final InputStream is) {
        if (is == null) return;
        try {
            is.close();
        } catch (Exception ign) {
        }
    }



}
