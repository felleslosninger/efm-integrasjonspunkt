package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.adresseregister.AdressRegisterFactory;
import no.difi.meldingsutveksling.dokumentpakking.kvit.Kvittering;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Created by kubkaray on 08.12.2014.
 */


public class SignAFileTest {
    @Ignore
    @Test
    public void signIt(){
        File f= new File(getClass().getClassLoader().getResource("kvitteringSbd.xml").getFile());
        StandardBusinessDocument sbd;
        JAXBElement<StandardBusinessDocument> standardBusinessDocumentJAXBElement;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class);
             standardBusinessDocumentJAXBElement= (JAXBElement<StandardBusinessDocument>) jaxbContext.createUnmarshaller().unmarshal(f);

        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        Certificate certificate = (Certificate) AdressRegisterFactory.createAdressRegister().getCertificate("958935429");
        Noekkelpar noekkelpar;
        try {
            noekkelpar= new Noekkelpar(loadPrivateKey(),certificate);
        } catch (IOException e) {
          throw  new RuntimeException(e);
        }
        Avsender avsender = new Avsender(new Organisasjonsnummer("958935429"),noekkelpar);
        SignAFile signAFile = new SignAFile();
        Kvittering signedFile = signAFile.signIt(standardBusinessDocumentJAXBElement.getValue().getAny(), avsender, KvitteringType.LEVERING);
        org.junit.Assert.assertNotNull(signedFile);
    }

    public PrivateKey loadPrivateKey() throws IOException {
        PrivateKey key = null;
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream("difi-privkey.pem");
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
          throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
          throw new RuntimeException(e);
        } finally {
            if (null != is){
                is.close();
            }
        }
        return key;
    }

}
