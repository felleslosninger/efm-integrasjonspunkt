package no.difi.meldingsutveksling.oxalisexchange;

import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Created by kubkaray on 18.12.2014.
 */
public class SbdCms {
    @Test
   public void run () throws IOException {
        File file = new File(getClass().getClassLoader().getResource("heyyoSbd.xml").getFile());
        String WRITE_TO = System.getProperty("user.home") + File.separator + "testToRemove" + File.separator + "blabla.zip";

        File fileTo = new File(WRITE_TO);
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class);
        } catch (JAXBException e) {

            throw new MeldingsUtvekslingRuntimeException(e);
        }
        Unmarshaller unmarshaller = null;
        try {
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {

            throw new MeldingsUtvekslingRuntimeException(e);
        }
        StandardBusinessDocument standardBusinessDocument =null;
        try {
            standardBusinessDocument =   unmarshaller.unmarshal(new StreamSource(file),StandardBusinessDocument.class).getValue();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }

        Payload payld= (Payload) standardBusinessDocument.getAny();
        String coded=payld.getContent();
        byte[] decoded=DatatypeConverter.parseBase64Binary(coded);
        CmsUtil cmsUtil = new CmsUtil();
      byte[] bit=  cmsUtil.decryptCMS(decoded,loadPrivateKey());
        FileUtils.writeByteArrayToFile(fileTo,bit);
    }

    public PrivateKey loadPrivateKey() throws IOException {
        PrivateKey key = null;
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream("difi-key.pem");
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
            throw new MeldingsUtvekslingRuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        } finally {
            if (null != is){
                is.close();
            }
        }
        return key;
    }
}
