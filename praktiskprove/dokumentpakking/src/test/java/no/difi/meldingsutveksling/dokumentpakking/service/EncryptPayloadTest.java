package no.difi.meldingsutveksling.dokumentpakking.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import no.difi.meldingsutveksling.adresseregmock.AdressRegisterFactory;
import no.difi.meldingsutveksling.dokumentpakking.domain.EncryptedContent;
import no.difi.meldingsutveksling.dokumentpakking.domain.Mottaker;
import no.difi.meldingsutveksling.dokumentpakking.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

public class EncryptPayloadTest {
	final PublicKey mottakerpublicKey = AdressRegisterFactory.createAdressRegister().getPublicKey("958935429");
	final String mottakerPrivateKey = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAN4tj2Uj2OkNJMSN"
			+ "aS6Vaj2CtZDSUiOrYRelXimOWjyMgADj7PjuipieaAyANkVr58b9XcdH4ow2KSW0" + "wUh6kM6P1ESGl39blzwFmq6BRPOhDqWmPijWrAqDM6uDeYBJSnxgan4PZ3I1eRJq"
			+ "ICw6VDrsmFqnRpknGKVgIYQPTSWTAgMBAAECgYBeh6v3MGVd4wW9yxzxgQkO2so9" + "r/7axlQtJ2ME81hZYr4jotZ0o6m8fclvaC2vI9YdyDdaTq+JUJH5RQrnt55cOcr+"
			+ "1TLffeWVoivOZXwAqyUhCxPCkA8b4LO1oK5kXDbVyc2lV/0xFLmAU07DE2p1DYaD" + "CIh2jZzsuBwj7EPUAQJBAPAzyX9VVXWlsx/H7Pa0PggB6Xo4czn+MTDv56X3aDRk"
			+ "XUtqukRFIcjcy6l5Zl7ER4CVu3aswgtGw40ds0Dji4ECQQDsyk2QEyayOhFwLziD" + "h29tS6QK7U9WqysuDx5sCDxXMT1MtsQlTcj4W02Ak8PRYDS3ccdpMlMttYKXLy+W"
			+ "C0sTAkBsVn9AXkWwTW8wG2VGlF8SD4K17HYUJxEayGnL0n3+e3IUzOt8VU36oZN+" + "OdIxVggF+ALYcO0IVv9mS4oI71iBAkByWawlVKpOTa6YL6WqFyCfdnTs9fdnklfS"
			+ "8WguobeKH/RLdMO6hBr2nRkLa9CX707l/CNh0PTMUSiUnCvt2NxTAkBPwCWmARS4" + "cZjrWFtnjw4mUjH+fR//WnLqYRFETNasROMr64uX+rtNxrvCXI4VB0oiuvKHwXd3"
			+ "uc9j/4wX04Kk";

	@Test
	public void testEncrypt() throws Exception {
		EncryptedContent c = new EncryptPayload()
				.encrypt("Encrypted content".getBytes(), new Mottaker(new Organisasjonsnummer("958935429"), mottakerpublicKey));
		
		Cipher keyCipher = Cipher.getInstance("RSA");
		Cipher contentCipher = Cipher.getInstance("AES");

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(mottakerPrivateKey));
		KeyFactory kf = KeyFactory.getInstance("RSA");
		keyCipher.init(Cipher.DECRYPT_MODE, kf.generatePrivate(keySpec));
		byte[] decryptedContentKey = keyCipher.doFinal(c.getKey());

		Key contentKey = new SecretKeySpec(decryptedContentKey, "AES");

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
		
		Cipher keyCipher = Cipher.getInstance("RSA");
		Cipher contentCipher = Cipher.getInstance("AES");
		
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(mottakerPrivateKey));
		KeyFactory kf = KeyFactory.getInstance("RSA");
		keyCipher.init(Cipher.DECRYPT_MODE, kf.generatePrivate(keySpec));
		byte[] decryptedContentKey = keyCipher.doFinal(Base64.decodeBase64(kopi.getEncryptionKey()));

		Key contentKey = new SecretKeySpec(decryptedContentKey, "AES");

		contentCipher.init(Cipher.DECRYPT_MODE, contentKey);
		byte[] decryptedContent = contentCipher.doFinal(Base64.decodeBase64(kopi.getAsice()));
		
		assertThat(new String(decryptedContent), is(equalTo("Encrypted content")));
	}
	


}
