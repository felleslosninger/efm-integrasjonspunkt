package no.difi.meldingsutveksling.dokumentpakking;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import no.difi.meldingsutveksling.dokumentpakking.crypto.Noekkelpar;
import no.difi.meldingsutveksling.dokumentpakking.crypto.Sertifikat;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Avsender;
import no.difi.meldingsutveksling.dokumentpakking.domain.Mottaker;
import no.difi.meldingsutveksling.dokumentpakking.domain.Organisasjonsnummer;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class PakkeTest {

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
	private InputStream keyStoreFile = PakkeTest.class.getClassLoader().getResourceAsStream("klient.jks");
	private String keyStoreType = "jks";

	private String keyStorePassword = "123456";
	Dokumentpakker datapakker = new Dokumentpakker();


	@Test
	public void testPakkingAvXML() throws IOException {
		KeyStore ks = null;
		try {
			ks = KeyStore.getInstance(keyStoreType);
			ks.load(keyStoreFile, keyStorePassword.toCharArray());

		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
		}

		Avsender avsender = Avsender.builder(new Organisasjonsnummer("12345678"), Noekkelpar.fraKeyStore(ks, "klient", "123456")).build();
		

		ByteArrayInputStream is = new ByteArrayInputStream(datapakker.pakkDokumentISbd(forsendelse, avsender, new Mottaker(new Organisasjonsnummer("12345678"), Sertifikat.fraKeyStore(ks, "server"))));
		IOUtils.copy(is, System.out);

	}
}
