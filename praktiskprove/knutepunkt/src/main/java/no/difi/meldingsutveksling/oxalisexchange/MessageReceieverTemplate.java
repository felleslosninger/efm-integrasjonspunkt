package no.difi.meldingsutveksling.oxalisexchange;

import no.difi.meldingsutveksling.domain.BestEduMessage;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.eventlog.ProcessState;
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
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public abstract class MessageReceieverTemplate {

    private static final String PRIVATE_KEY_FILE = "958935429-oslo-kommune.pkcs8";
    private static final String PAYLOAD_ZIP = System.getProperty("user.home")+ File.separator +"testToRemove" + File.separator + "payload.zip";
    private static final String PAYLOAD = "payload";
    private static final int MAGIC_NR = 1024;

    private EventLog eventLog = EventLog.create();
    private List<Object> toRemoveAfterIntegration = new ArrayList();

    abstract void sendLeveringskvittering(Map list);

    abstract void sendApningskvittering(Map list);

    public void receive(Document document) throws GeneralSecurityException {
        Node n = null;
        Map documentElements = null;
        try {
            documentElements = documentMapping(document);
            n = (Node) documentElements.get("DocumentIdentification");
        } catch (JAXBException e) {
            eventLog.log(new Event());
        }

        eventLog.log(new Event().setProcessStates(ProcessState.MESSAGE_RECIEVED));

        if (isSBD(n)) {

            eventLog.log(new Event().setProcessStates(ProcessState.SBD_RECIEVED));
            try {
                documentElements.put("privateKey", loadPrivateKey());
            } catch (IOException e) {
                eventLog.log(new Event().setExceptionMessage(e.toString()));
            }
            sendLeveringskvittering(documentElements);
            eventLog.log(new Event().setProcessStates(ProcessState.LEVERINGS_KVITTERING_SENT));

            // get payloaed and encryption key
            Node payload = (Node) documentElements.get(PAYLOAD);


            // dekryptert payload (AES)
            byte[] asicFileBytes = new byte[0];
            try {
                asicFileBytes = getZipBytesFromDocument(payload);
            } catch (IOException e) {
                eventLog.log(new Event().setExceptionMessage(e.toString()));
            }
            eventLog.log(new Event().setProcessStates(ProcessState.DECRYPTION_SUCCESS));

            // Signaturvalidering
            ZipFile asicFile = verifySignature(asicFileBytes, payload);
            System.out.println(asicFile.entries());
            eventLog.log(new Event().setProcessStates(ProcessState.SIGNATURE_VALIDATED));

            BestEduMessage bestEduMessage = getBestEduFromAsic(asicFile);
            senToNoark(bestEduMessage,documentElements);
            eventLog.log(new Event().setProcessStates(ProcessState.BEST_EDU_SENT));


            eventLog.log(new Event().setProcessStates(ProcessState.AAPNINGS_KVITTERING_SENT));

        } else {
            // BestEdu recieved
            eventLog.log(new Event().setProcessStates(ProcessState.BEST_EDU_RECIEVED));
        }
    }




    /**
     * Maps out document elements as nodes
     *
     * @param document sbd or best/edu
     * @return List of node extended objects
     * @throws JAXBException
     */
    private Map<String, Node> documentMapping(Document document) throws JAXBException {

        Map list = new HashMap();
        NodeList sbdhNodes = document.getElementsByTagName("ns2:StandardBusinessDocumentHeader");
        Node sbdhElement = sbdhNodes.item(0);
        NodeList payloadNodes = document.getElementsByTagName(PAYLOAD);
        NodeList childs = sbdhElement.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node n = childs.item(i);
            String name = n.getNodeName();
            if (name.contains("ns2:")) {
                name = name.replace("ns2:", "");
            }
            if (name.equals("#text"))
                continue;

            list.put(name, n);
        }
        for (int i = 0; i < payloadNodes.getLength(); i++) {
            Node n = payloadNodes.item(i);
            list.put(PAYLOAD, n);
        }

        return list;
    }


    protected void senToNoark(BestEduMessage bestEduMessage,Map documentElements) {
       //TODO:dette er implimentert men komentert bort med hensyn på debugging .
      //  sendApningskvittering(documentElements);
    }


    private BestEduMessage getBestEduFromAsic(ZipFile asicFile) {

        return null;
    }

    private ZipFile verifySignature(byte[] aesKey, Node payload) {
        String payloadTextContent = payload.getTextContent();
        byte[] aesEncZip = DatatypeConverter.parseBase64Binary(payloadTextContent);
        ZipFile zipFile = null;
        //*** get aes cipher decrypt *****
        Cipher aesCipher = null;
        try {
            aesCipher = Cipher.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
        } catch (NoSuchPaddingException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, "AES");
        SecureRandom secureRandom = null;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
        }

        try {
            aesCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, secureRandom);
        } catch (InvalidKeyException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
        }
        byte[] zipTobe = new byte[0];
        try {
            zipTobe = aesCipher.doFinal(aesEncZip);
        } catch (IllegalBlockSizeException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
        } catch (BadPaddingException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
        }

        File file = new File(PAYLOAD_ZIP);
        file.setWritable(true, false);
        try {
            FileUtils.writeByteArrayToFile(file, zipTobe);
        } catch (IOException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
        }
        try {
            zipFile = new ZipFile(file);
        } catch (IOException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
        }
        return zipFile;
    }

    private byte[] getZipBytesFromDocument(Node payload) throws GeneralSecurityException, IOException {
        NamedNodeMap namedNodeMap = payload.getAttributes();
        String aesInRsa = namedNodeMap.getNamedItem("encryptionKey").getTextContent();
        byte[] aesInDisc = DatatypeConverter.parseBase64Binary(aesInRsa);

        //*** get rsa cipher decrypt *****
        Cipher cipher = Cipher.getInstance("RSA");
        PrivateKey privateKey = loadPrivateKey();
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(aesInDisc);
    }

    private boolean isSBD(Node node) {
        return node.getTextContent().toLowerCase().contains("sbd");
    }

    /**
     * Loads the private key from a pkcs8 file
     *
     * @return an private key
     * @throws java.io.IOException
     */
    public PrivateKey loadPrivateKey() throws IOException {
        PrivateKey key = null;
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(PRIVATE_KEY_FILE);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            boolean inKey = false;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!inKey && line.startsWith("-----BEGIN ") &&
                        line.endsWith(" PRIVATE KEY-----")) {
                    inKey = true;
                } else {
                    if (line.startsWith("-----END ") &&
                            line.endsWith(" PRIVATE KEY-----")) {
                        inKey = false;
                    }
                    builder.append(line);
                }
            }

            byte[] encoded = DatatypeConverter.parseBase64Binary(builder.toString());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            key = kf.generatePrivate(keySpec);

        } catch (InvalidKeySpecException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
        } catch (NoSuchAlgorithmException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
        } finally {
            if (null != is){
                is.close();
            }
        }
        return key;
    }

    public void unZipIt(String zipFile, String outputFolder) {

        byte[] buffer = new byte[MAGIC_NR];

        try {

            //create output directory is not exists
            File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }

            //get the zip file content
            ZipInputStream zis =
                    new ZipInputStream(new FileInputStream(zipFile));
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {

                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

        } catch (IOException ex) {
            eventLog.log(new Event().setExceptionMessage(ex.toString()));
        }
    }

}
