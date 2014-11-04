
package no.difi.messagehandler;

import org.bouncycastle.cms.CMSException;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;

import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.PKCS8EncodedKeySpec;


/**
 * Created by kons-kka on 30.10.2014.
 */
public class MessageHandler {

    private String pemFileName ="958935429-oslo-kommune.pem";

    void unmarshall(File sdbXml) throws JAXBException, GeneralSecurityException, IOException, CMSException {
       //*** Unmarshall xml*****
        JAXBContext jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        StandardBusinessDocument standardBusinessDocument = (StandardBusinessDocument) unmarshaller.unmarshal(sdbXml);

        //*** get payload
        Payload payload= (Payload) standardBusinessDocument.getAny();
        byte[] payloadBytes=payload.asice;
        String result = DatatypeConverter.printBase64Binary(payloadBytes);




        //*** get rsa cipher decrypt
        Cipher cipher = Cipher.getInstance("RSA");
        PrivateKey privateKey =loadPrivateKey();
        cipher.init(Cipher.DECRYPT_MODE,privateKey);
        byte[] utf8 = cipher.doFinal(result.getBytes());
        String decrypted= new String(utf8,"UTF8");
        System.out.println();
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
            byte[] encoded = DatatypeConverter.parseBase64Binary(pkcs8key);
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

    private static PrivateKey getPrivateKey(String privateKeyFileNameLocation) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException, KeyStoreException, IOException, CertificateException, UnrecoverableEntryException {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load((KeyStore.LoadStoreParameter) new FileInputStream(privateKeyFileNameLocation));
        String alias = (String) ks.aliases().nextElement();
        PrivateKey prvtk= (PrivateKey) ks.getKey("server","123456".toCharArray());
        return prvtk;
        /*KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, new KeyStore.PasswordProtection(passPhrase.toCharArray()));
        return keyEntry.getPrivateKey();*/
    }

    public void codeDecode() {
       //TODO: finn ut om public nøkkelen oppdatert
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "payload")
    public static class Payload {
        @XmlValue
        byte[] asice;

        @XmlAttribute
        String encoding;

        @XmlAttribute
        String type;
    }

}
