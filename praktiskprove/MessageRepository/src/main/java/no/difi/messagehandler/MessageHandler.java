
package no.difi.messagehandler;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;


import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;


/**
 * Created by kons-kka on 30.10.2014.
 */
public class MessageHandler {

    void unmarshall(File sdbXml) throws JAXBException, NoSuchPaddingException, NoSuchAlgorithmException, CertificateException, KeyStoreException, UnrecoverableEntryException, IOException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, CMSException, NoSuchProviderException {
       //*** Unmarshall xml*****
        JAXBContext jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        StandardBusinessDocument standardBusinessDocument = (StandardBusinessDocument) unmarshaller.unmarshal(sdbXml);

        //*** get payload
        Payload payload= (Payload) standardBusinessDocument.getAny();
        byte[] payloadBytes=payload.asice;
        byte[] result = DatatypeConverter.parseBase64Binary(new String(payloadBytes));

        Cipher cipher = Cipher.getInstance("RSA");
        URL file=PakkeTest.class.getClassLoader().getResourceAsStream("958935429-oslo-kommune.pem");


        cipher.init(Cipher.DECRYPT_MODE,getPrivateKey(prop));
        byte[] utf8 =cipher.doFinal(result);
        String decrypted= new String(utf8,"UTF8");
        System.out.println();
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
