package no.difi.meldingsutveksling.dokumentpakking;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import no.difi.meldingsutveksling.adresseregmock.AdressRegisterFactory;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Avsender;
import no.difi.meldingsutveksling.dokumentpakking.domain.Mottaker;
import no.difi.meldingsutveksling.dokumentpakking.domain.Organisasjonsnummer;

import org.junit.Test;

public class PakkeTest {
	final String mottakerPrivateKey = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAN4tj2Uj2OkNJMSN"
			+ "aS6Vaj2CtZDSUiOrYRelXimOWjyMgADj7PjuipieaAyANkVr58b9XcdH4ow2KSW0" + "wUh6kM6P1ESGl39blzwFmq6BRPOhDqWmPijWrAqDM6uDeYBJSnxgan4PZ3I1eRJq"
			+ "ICw6VDrsmFqnRpknGKVgIYQPTSWTAgMBAAECgYBeh6v3MGVd4wW9yxzxgQkO2so9" + "r/7axlQtJ2ME81hZYr4jotZ0o6m8fclvaC2vI9YdyDdaTq+JUJH5RQrnt55cOcr+"
			+ "1TLffeWVoivOZXwAqyUhCxPCkA8b4LO1oK5kXDbVyc2lV/0xFLmAU07DE2p1DYaD" + "CIh2jZzsuBwj7EPUAQJBAPAzyX9VVXWlsx/H7Pa0PggB6Xo4czn+MTDv56X3aDRk"
			+ "XUtqukRFIcjcy6l5Zl7ER4CVu3aswgtGw40ds0Dji4ECQQDsyk2QEyayOhFwLziD" + "h29tS6QK7U9WqysuDx5sCDxXMT1MtsQlTcj4W02Ak8PRYDS3ccdpMlMttYKXLy+W"
			+ "C0sTAkBsVn9AXkWwTW8wG2VGlF8SD4K17HYUJxEayGnL0n3+e3IUzOt8VU36oZN+" + "OdIxVggF+ALYcO0IVv9mS4oI71iBAkByWawlVKpOTa6YL6WqFyCfdnTs9fdnklfS"
			+ "8WguobeKH/RLdMO6hBr2nRkLa9CX707l/CNh0PTMUSiUnCvt2NxTAkBPwCWmARS4" + "cZjrWFtnjw4mUjH+fR//WnLqYRFETNasROMr64uX+rtNxrvCXI4VB0oiuvKHwXd3"
			+ "uc9j/4wX04Kk";
	final PublicKey mottakerpublicKey = new AdressRegisterFactory().createAdressRegister().getPublicKey("958935429");
	private AsicEAttachable forsendelse = new AsicEAttachable() {
		@Override
		public String getMimeType() {
			return "text/xml";
		}

		@Override
		public String getFileName() {
			return "xml.xml";
		}

		@Override
		public byte[] getBytes() {
			return "<root><body></body></root>".getBytes();
		}
	};
	Dokumentpakker datapakker = new Dokumentpakker();

	@Test
	public void testPakkingAvXML() throws IOException, InvalidKeySpecException, KeyStoreException, NoSuchAlgorithmException, CertificateException {

		Avsender avsender = Avsender.builder(new Organisasjonsnummer("12345678"), null).build();

		ByteArrayInputStream is = new ByteArrayInputStream(datapakker.pakkDokumentISbd(forsendelse, avsender, new Mottaker(
				new Organisasjonsnummer("958935429"), mottakerpublicKey)));
		
		//IOUtils.copy(is, System.out);
		 
//		 FileUtils.writeByteArrayToFile(new File("c:\\sbd.xml"), datapakker.pakkDokumentISbd(forsendelse, avsender, new Mottaker(
//					new Organisasjonsnummer("958935429"), mottakerpublicKey)));
	}
}
