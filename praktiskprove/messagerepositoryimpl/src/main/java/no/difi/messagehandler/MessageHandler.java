
package no.difi.messagehandler;


import org.apache.commons.io.FileUtils;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.Partner;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.PartnerIdentification;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * @author Kubilay Karayilan
 *         Kubilay.Karayilan@inmeta.no
 *         created on 30.10.2014.
 */
public class MessageHandler {

    private static final String PAYLOAD_ZIP = System.getProperty("user.home") + File.separator +"testToRemove"+File.separator+ "payload.zip";
    private static final int MAGIC_NR = 1024;
    private String pemFileName = "960885406-statens-laanekasse.pkcs8";
    private String publicKeyFileName = "960885406-statens-laanekasse.publickey";
    private static final String outputFolder = "C:" + File.separator + "output.zip";
    private String payloadExtractDestination =  System.getProperty("user.home") + File.separator+"testToRemove"+
            File.separator+ "Zip Output";
    private java.lang.String RSA_INSTANCE = "RSA";
    private List<Exception> list;

    /**
     * Unmarshalls the SBD file
     *
     * @param sdbXml Standard business document
     * @throws JAXBException
     * @throws GeneralSecurityException
     * @throws IOException
     *
     */
    void unmarshall(File sdbXml) throws JAXBException, GeneralSecurityException, IOException {
        //*** Unmarshall xml*****
        JAXBContext jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        StandardBusinessDocument standardBusinessDocument = (StandardBusinessDocument) unmarshaller.unmarshal(sdbXml);

        //*** query to Elma to get PK
        List<Partner> senders = standardBusinessDocument.getStandardBusinessDocumentHeader().getSenders();
        Partner sender = senders.get(0);
        PartnerIdentification orgNr = sender.getIdentifier();
        String[] orgNrArr = orgNr.getValue().split(":");

        //*** get payload *****
        Payload payload = (Payload) standardBusinessDocument.getAny();
        String aesInRsa = payload.encryptionKey;
        String payloadString = payload.asice;
        byte[] aesInDisc = DatatypeConverter.parseBase64Binary(aesInRsa);
        byte[] aesEncZip = DatatypeConverter.parseBase64Binary(payloadString);

        //*** get rsa cipher decrypt *****
        Cipher cipher = Cipher.getInstance(RSA_INSTANCE);
        PrivateKey privateKey = loadPrivateKey();
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] aesKey = cipher.doFinal(aesInDisc);


        //*** get aes cipher decrypt *****
        Cipher aesCipher = Cipher.getInstance("AES");
        SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, "AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");

        aesCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, secureRandom);
        byte[] zipTobe = aesCipher.doFinal(aesEncZip);

        File file = new File(PAYLOAD_ZIP);
        FileUtils.writeByteArrayToFile(file, zipTobe);
        unZipIt(PAYLOAD_ZIP, payloadExtractDestination);

    }

    /**
     * Unzips the Zip payload
     *
     * @param zipFile      payload
     * @param outputFolder destination folder
     */
    public void unZipIt(String zipFile, String outputFolder) {

        byte[] buffer = new byte[MAGIC_NR];

        try {

            //create output directory is not exists
            File folder = new File(MessageHandler.outputFolder);
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
           list.add(ex);
        }
    }


    /**
     * Loads the private key from a pkcs8 file
     *
     * @return an private key
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public PrivateKey loadPrivateKey()
            throws IOException, GeneralSecurityException {
        PrivateKey key = null;
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(pemFileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            boolean inKey = false;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!inKey) {
                    if (line.startsWith("-----BEGIN ") &&
                            line.endsWith(" PRIVATE KEY-----")) {
                        inKey = true;
                    }
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
            is.close();
        }
        return key;
    }


    /**
     * This part belongs to cryptography testing
     * takes in a text and crypts it with public key
     * then decrypts it with private key
     *
     * @param text text to crypt
     * @return decrypted text
     * @throws GeneralSecurityException
     * @throws IOException
     */

    public String cryptAtext(String text) throws GeneralSecurityException, IOException {

        Cipher cipher = Cipher.getInstance("RSA");
        PublicKey publicKey = getPublicKey();
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedMessege = cipher.doFinal(text.getBytes());
        return decode(encryptedMessege);
    }

    /**
     * Decodes encrypted byte array
     *
     * @param encrypted
     * @return decrypted text
     * @throws GeneralSecurityException
     * @throws IOException
     */
    private String decode(byte[] encrypted) throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance("RSA");
        PrivateKey privateKey = loadPrivateKey();
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] utf8 = cipher.doFinal(encrypted);
        String decrypted = new String(utf8, "UTF8");
        return decrypted;
    }

    /**
     * Extracts a public key from a pem file
     *
     * @return Public key
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws InvalidKeySpecException
     */
    private PublicKey getPublicKey() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {

        KeyFactory kf = KeyFactory.getInstance("RSA");
        InputStream is = null;
        StringBuilder builder = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(publicKeyFileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            builder = new StringBuilder();
            boolean inKey = false;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!inKey) {
                    if (line.startsWith("-----BEGIN ") &&
                            line.endsWith(" PUBLIC KEY-----")) {
                        inKey = true;
                    }
                    continue;
                } else {
                    if (line.startsWith("-----END ") &&
                            line.endsWith(" PUBLIC KEY-----")) {
                        inKey = false;
                        break;
                    }
                    builder.append(line);
                }
            }
        } finally {
            is.close();
        }
        byte[] keyBytes = DatatypeConverter.parseBase64Binary(builder.toString());
        ;
        X509EncodedKeySpec keySpec =
                new X509EncodedKeySpec(keyBytes);
        PublicKey key = kf.generatePublic(keySpec);
        return key;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "payload")
    public static class Payload {
        @XmlValue
        String asice;

        @XmlAttribute
        String encoding;

        @XmlAttribute
        String type;

        @XmlAttribute
        String encryptionKey;
    }

}
