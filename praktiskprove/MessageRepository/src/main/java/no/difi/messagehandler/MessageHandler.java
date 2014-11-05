
package no.difi.messagehandler;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.cms.CMSException;
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
 * Created by kons-kka on 30.10.2014.
 */
public class MessageHandler {

    private static final String PAYLOAD_ZIP = "C:"+File.separator+"payload.zip";
    private String pemFileName ="958935429-oslo-kommune.pkcs8";
    private String publicKeyFileName= "958935429-oslo-kommune.publickey";
    private static final String OUTPUT_FOLDER = "C:"+File.separator+"output.zip";
    private String PAYLOAD_EXTRACT_DESTINATION ="C:"+File.separator+"Zip Output";

    void unmarshall(File sdbXml) throws JAXBException, GeneralSecurityException, IOException, CMSException {
       //*** Unmarshall xml*****
        JAXBContext jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        StandardBusinessDocument standardBusinessDocument = (StandardBusinessDocument) unmarshaller.unmarshal(sdbXml);

        //*** query to Elma to get PK etc..
        new AdressRegisterFactory().createAdressRegister().getPublicKey("958935429");

        //*** get payload *****
        Payload payload= (Payload) standardBusinessDocument.getAny();
        String aesInRsa=payload.encryptionKey;
        String payloadString=payload.asice;
        byte[] aesInDisc = DatatypeConverter.parseBase64Binary(aesInRsa);
        byte[] aesEncZip = DatatypeConverter.parseBase64Binary(payloadString);

        //*** get rsa cipher decrypt *****
        Cipher cipher = Cipher.getInstance("RSA");
        PrivateKey privateKey =loadPrivateKey();
        cipher.init(Cipher.DECRYPT_MODE,privateKey);
        byte[] aesKey = cipher.doFinal(aesInDisc);


        //*** get aes cipher decrypt *****
        Cipher aesCipher = Cipher.getInstance("AES");
        SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey,"AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        aesCipher.init(Cipher.DECRYPT_MODE,secretKeySpec,secureRandom);
        byte[] zipTobe= aesCipher.doFinal( aesEncZip);

        File file = new File (PAYLOAD_ZIP);
        file.setWritable(true,false);
        FileUtils.writeByteArrayToFile(file, zipTobe);
        unZipIt(PAYLOAD_ZIP, PAYLOAD_EXTRACT_DESTINATION);

    }
    public void unZipIt(String zipFile, String outputFolder){

        byte[] buffer = new byte[1024];

        try{

            //create output directory is not exists
            File folder = new File(OUTPUT_FOLDER);
            if(!folder.exists()){
                folder.mkdir();
            }

            //get the zip file content
            ZipInputStream zis =
                    new ZipInputStream(new FileInputStream(zipFile));
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while(ze!=null){

                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

                System.out.println("file unzip : "+ newFile.getAbsoluteFile());

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

            System.out.println("Done");

        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    public PrivateKey loadPrivateKey( )
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
                    continue;
                }
                else {
                    if (line.startsWith("-----END ") &&
                            line.endsWith(" PRIVATE KEY-----")) {
                        inKey = false;
                        break;
                    }
                    builder.append(line);
                }
            }
            String pkcs8key =
                    "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAN4tj2Uj2OkNJMSN" +
                    "aS6Vaj2CtZDSUiOrYRelXimOWjyMgADj7PjuipieaAyANkVr58b9XcdH4ow2KSW0" +
                    "wUh6kM6P1ESGl39blzwFmq6BRPOhDqWmPijWrAqDM6uDeYBJSnxgan4PZ3I1eRJq" +
                    "ICw6VDrsmFqnRpknGKVgIYQPTSWTAgMBAAECgYBeh6v3MGVd4wW9yxzxgQkO2so9" +
                    "r/7axlQtJ2ME81hZYr4jotZ0o6m8fclvaC2vI9YdyDdaTq+JUJH5RQrnt55cOcr+" +
                    "1TLffeWVoivOZXwAqyUhCxPCkA8b4LO1oK5kXDbVyc2lV/0xFLmAU07DE2p1DYaD" +
                    "CIh2jZzsuBwj7EPUAQJBAPAzyX9VVXWlsx/H7Pa0PggB6Xo4czn+MTDv56X3aDRk" +
                    "XUtqukRFIcjcy6l5Zl7ER4CVu3aswgtGw40ds0Dji4ECQQDsyk2QEyayOhFwLziD" +
                    "h29tS6QK7U9WqysuDx5sCDxXMT1MtsQlTcj4W02Ak8PRYDS3ccdpMlMttYKXLy+W" +
                    "C0sTAkBsVn9AXkWwTW8wG2VGlF8SD4K17HYUJxEayGnL0n3+e3IUzOt8VU36oZN+" +
                    "OdIxVggF+ALYcO0IVv9mS4oI71iBAkByWawlVKpOTa6YL6WqFyCfdnTs9fdnklfS" +
                    "8WguobeKH/RLdMO6hBr2nRkLa9CX707l/CNh0PTMUSiUnCvt2NxTAkBPwCWmARS4" +
                    "cZjrWFtnjw4mUjH+fR//WnLqYRFETNasROMr64uX+rtNxrvCXI4VB0oiuvKHwXd3" +
                    "uc9j/4wX04Kk";
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
        try { is.close(); } catch (Exception ign) {}
    }


    public String cryptAtext(String text) throws GeneralSecurityException, IOException {

        System.out.println("incoming text before encryption: "+ text+ "...");
        Cipher cipher = Cipher.getInstance("RSA");
        PublicKey publicKey=getPublicKey();
        cipher.init(Cipher.ENCRYPT_MODE,publicKey);
        byte [] encryptedMessege= cipher.doFinal(text.getBytes());
        System.out.println("incoming text after encryption: "+DatatypeConverter.printBase64Binary(encryptedMessege)+ "...");
        return  decode(encryptedMessege);
    }

    private String decode(byte[] encrypted) throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance("RSA");
        PrivateKey privateKey =loadPrivateKey();
        cipher.init(Cipher.DECRYPT_MODE,privateKey);
        byte[] utf8 = cipher.doFinal(encrypted);
        String decrypted= new String(utf8,"UTF8");
        System.out.println("Decrypted message: " + decrypted+ "...");
        return decrypted;
    }

    private PublicKey getPublicKey() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {

        KeyFactory kf = KeyFactory.getInstance("RSA");
        InputStream is = null;
        StringBuilder builder=null;
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
                }
                else {
                    if (line.startsWith("-----END ") &&
                            line.endsWith(" PUBLIC KEY-----")) {
                        inKey = false;
                        break;
                    }
                    builder.append(line);
                }
            }
        }finally {
            closeSilent(is);
        }
        byte[] keyBytes =DatatypeConverter.parseBase64Binary(builder.toString());;
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
