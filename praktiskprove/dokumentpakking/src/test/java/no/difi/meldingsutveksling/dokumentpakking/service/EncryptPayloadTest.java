package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.adresseregister.AdressRegisterFactory;
import no.difi.meldingsutveksling.dokumentpakking.domain.EncryptedContent;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import java.io.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class EncryptPayloadTest {
	final PublicKey mottakerpublicKey = AdressRegisterFactory.createAdressRegister().getPublicKey("958935429");

	@Test
	public void testEncrypt() throws Exception {
		EncryptedContent c = new EncryptPayload()
				.encrypt("Encrypted content".getBytes(), new Mottaker(new Organisasjonsnummer("958935429"), mottakerpublicKey));
		
		Cipher keyCipher = Cipher.getInstance(EncryptPayload.RSA_MODE);
		Cipher contentCipher = Cipher.getInstance(EncryptPayload.AES_MODE);

		keyCipher.init(Cipher.DECRYPT_MODE, loadPrivateKey("958935429-oslo-kommune.pkcs8"));
		byte[] decryptedContentKey = keyCipher.doFinal(c.getKey());

		Key contentKey = new SecretKeySpec(decryptedContentKey, EncryptPayload.AES_MODE);

		contentCipher.init(Cipher.DECRYPT_MODE, contentKey);
		byte[] decryptedContent = contentCipher.doFinal(c.getContent());

		assertThat(new String(decryptedContent), is(equalTo("Encrypted content")));
	}
	
	@Test
	public void testEncryptionThroughMarshallinging() throws Exception {
		EncryptedContent c = new EncryptPayload()
				.encrypt("Encrypted content".getBytes(), new Mottaker(new Organisasjonsnummer("958935429"), mottakerpublicKey));

		Payload p = new Payload(c);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		JAXBContext jaxbContext = JAXBContext.newInstance(Payload.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		jaxbMarshaller.marshal(p, os);
		

		InputStream is = new ByteArrayInputStream(os.toByteArray());
		
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Payload kopi = (Payload) jaxbUnmarshaller.unmarshal(is);
		
		Cipher keyCipher = Cipher.getInstance(EncryptPayload.RSA_MODE);
		Cipher contentCipher = Cipher.getInstance(EncryptPayload.AES_MODE);
		
		keyCipher.init(Cipher.DECRYPT_MODE, loadPrivateKey("958935429-oslo-kommune.pkcs8"));
		byte[] decryptedContentKey = keyCipher.doFinal(Base64.decodeBase64(kopi.getEncryptionKey()));

		Key contentKey = new SecretKeySpec(decryptedContentKey, EncryptPayload.AES_MODE);

		contentCipher.init(Cipher.DECRYPT_MODE, contentKey);
		byte[] decryptedContent = contentCipher.doFinal(Base64.decodeBase64(kopi.getAsice()));
		
		assertThat(new String(decryptedContent), is(equalTo("Encrypted content")));
	}
	
	
    public PrivateKey loadPrivateKey(String fileName) throws IOException  {
        PrivateKey key = null;
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            boolean inKey = false;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!inKey &&  line.startsWith("-----BEGIN ") &&
                        line.endsWith(" PRIVATE KEY-----") ) {
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
            System.out.println(keySpec.getFormat());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            key = kf.generatePrivate(keySpec);

        } catch (InvalidKeySpecException e) {
        } catch (NoSuchAlgorithmException e) {
        } finally {
          is.close();
        }
        return key;
    }

}
