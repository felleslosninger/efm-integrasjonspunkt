
package no.difi.messagehandler;

import org.bouncycastle.cms.*;
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
        byte[] result = DatatypeConverter.parseBase64Binary(new String(payloadBytes));




        //*** get rsa cipher decrypt
        Cipher cipher = Cipher.getInstance("RSA");
        PrivateKey privateKey =loadPrivateKey();
        cipher.init(Cipher.DECRYPT_MODE,privateKey);
        byte[] utf8 =cipher.doFinal(result);
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
            //
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
