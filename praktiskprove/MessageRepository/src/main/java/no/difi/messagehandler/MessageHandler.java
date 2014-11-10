
package no.difi.messagehandler;

import ch.qos.logback.classic.Logger;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.cms.CMSException;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * @author Kubilay Karayilan
 *         Kubilay.Karayilan@inmeta.no
 *         created on 30.10.2014.
 */
public class MessageHandler {


    private static final String PAYLOAD_ZIP = "C:" + File.separator + "payload.zip";
    private static final int MAGIC_NUMBER = 1024;
    private static final String PEM_FILE_NAME = "958935429-oslo-kommune.pkcs8";
    private static final String PUBLIC_KEY_FILENAME = "958935429-oslo-kommune.publickey";
    private static final String OUTPUT_FOLDER = "C:" + File.separator + "output.zip";
    private static final String PAYLOAD_EXTRACT_DESTINATION = "C:" + File.separator + "Zip Output";
    private static final String RSA_INSTANCE = "RSA";
    private static final String AES_INSTANCE = "AES";
    private static final String ERROR_MESSAGE ="Couldnt decrypt!";
    private static final String PRIVATE_LINE_START = "-----BEGIN ";
    private static final String PRIVATE_LINE_END = " PRIVATE KEY-----";
    private static final String AMBIQUOUS_LINE_START = "-----END ";
    private static final String PUBLIC_LINE_END = " PUBLIC KEY-----";
    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(MessageHandler.class);

    /**
     * Unmarshalls the SBD file
     *
     * @param sdbXml Standard business document
     * @throws JAXBException
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws CMSException
     */
    void unmarshall(File sdbXml) throws JAXBException, GeneralSecurityException, IOException, CMSException {
        //*** Unmarshall xml*****
        JAXBContext jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        StandardBusinessDocument standardBusinessDocument = (StandardBusinessDocument) unmarshaller.unmarshal(sdbXml);
        //*** query to Elma to get PK
        //***   List<Partner> senders = standardBusinessDocument.getStandardBusinessDocumentHeader().getSenders();
        //***  Partner sender = senders.get(0);
        //*** PartnerIdentification orgNr = sender.getIdentifier();
        //*** String[] orgNrArr = orgNr.getValue().split(":"); ***
       //*** final PublicKey senderPublicKey = new AdressRegisterFactory().createAdressRegister().getPublicKey(orgNrArr[1]); ***

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
        Cipher aesCipher = Cipher.getInstance(AES_INSTANCE);
        SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, AES_INSTANCE);
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");

        aesCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, secureRandom);
        byte[] zipTobe = aesCipher.doFinal(aesEncZip);

        File file = new File(PAYLOAD_ZIP);
        file.setWritable(true, false);
        FileUtils.writeByteArrayToFile(file, zipTobe);
        unZipIt(PAYLOAD_ZIP, PAYLOAD_EXTRACT_DESTINATION);


    }

    /**
     * Unzips the Zip payload
     *
     * @param zipFile      payload
     * @param outputFolder destination folder
     */
    public void unZipIt(String zipFile, String outputFolder) throws IOException {

        byte[] buffer = new byte[MAGIC_NUMBER];


        //create output directory is not exists
        File folder = new File(OUTPUT_FOLDER);
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

    }


    /**
     * Loads the private key from a pkcs8 file
     *
     * @return an private key
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public PrivateKey loadPrivateKey()
            throws IOException {
        PrivateKey key = null;
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(PEM_FILE_NAME);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            StringBuilder stringBuilder= new StringBuilder();
            boolean inKey = false;
            for (String linen = bufferedReader.readLine(); linen != null; linen = bufferedReader.readLine()) {
                if (!inKey && linen.startsWith(PRIVATE_LINE_START) &&
                        linen.endsWith(PRIVATE_LINE_END)) {
                        inKey = true;
                } else {
                    if (linen.startsWith(AMBIQUOUS_LINE_START) &&
                            linen.endsWith(PRIVATE_LINE_END)) {
                        inKey = false;
                        break;
                    }
                    stringBuilder.append(linen);
                }
            }

            byte[] encoded = DatatypeConverter.parseBase64Binary(stringBuilder.toString());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            key = kf.generatePrivate(keySpec);

        } catch (InvalidKeySpecException e) {
           LOGGER.error("loadPrivateKey " + e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("loadPrivateKey " + e);
        } finally {
            closeSilent(is);
        }
        return key;
    }

    public static void closeSilent(final InputStream is) throws IOException {
        if (is == null) {
            return;
        }
        is.close();
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

    public String cryptAtext(String text) throws  IOException {
        byte [] encryptedMessege = new byte[0];
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            PublicKey publicKey = getPublicKey();
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            encryptedMessege = cipher.doFinal(text.getBytes());
            return decode(encryptedMessege);
        }catch (GeneralSecurityException gse){
            LOGGER.error("cryptAtext: " + gse);
        }
        return ERROR_MESSAGE;
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

        return new String(utf8, "UTF8");
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
            is = getClass().getClassLoader().getResourceAsStream(PUBLIC_KEY_FILENAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            builder = new StringBuilder();
            boolean inKey = false;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!inKey && line.startsWith(PRIVATE_LINE_START) &&
                        line.endsWith(PUBLIC_LINE_END)) {

                        inKey = true;

                } else {
                    if (line.startsWith(AMBIQUOUS_LINE_START) &&
                            line.endsWith(PUBLIC_LINE_END)) {
                        inKey = false;
                        break;
                    }
                    builder.append(line);
                }
            }
        } finally {
            closeSilent(is);
        }
        byte[] keyBytes = DatatypeConverter.parseBase64Binary(builder.toString());
        X509EncodedKeySpec keySpec =
                new X509EncodedKeySpec(keyBytes);

        return kf.generatePublic(keySpec);
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
