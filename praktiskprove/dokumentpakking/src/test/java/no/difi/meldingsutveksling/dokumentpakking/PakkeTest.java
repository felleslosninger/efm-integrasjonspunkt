package no.difi.meldingsutveksling.dokumentpakking;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import no.difi.meldingsutveksling.dokumentpakking.crypto.CreateSignature;
import no.difi.meldingsutveksling.dokumentpakking.crypto.Noekkelpar;
import no.difi.meldingsutveksling.dokumentpakking.crypto.Sertifikat;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.CMSDocument;
import no.difi.meldingsutveksling.dokumentpakking.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.dokumentpakking.domain.Payload;
import no.difi.meldingsutveksling.dokumentpakking.domain.TekniskAvsender;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSDocument;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMScryptadedAsic;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateZip;
import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD;

import org.junit.Test;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;

public class PakkeTest {

	private CreateSignature createSignature = new CreateSignature();
	private CreateZip createZip = new CreateZip();
	private final CreateCMSDocument createCMS = new CreateCMSDocument();
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
	private CreateCMScryptadedAsic createCMScryptadedAsic = new CreateCMScryptadedAsic(createSignature, createZip, createCMS);

	public CMSDocument createAsice() {

		List<AsicEAttachable> files = new ArrayList<AsicEAttachable>();
		files.add(forsendelse);

		KeyStore ks = null;
		try {
			ks = KeyStore.getInstance(keyStoreType);
			ks.load(keyStoreFile, keyStorePassword.toCharArray());

		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
		}

		TekniskAvsender avsender = TekniskAvsender.builder("12345678", Noekkelpar.fraKeyStore(ks, "klient", "123456")).build();

		CMSDocument cms = createCMScryptadedAsic.createAsice(forsendelse, avsender, Sertifikat.fraKeyStore(ks, "server"));

		return cms;
	}

	@Test
	public void testPakkingAvXML() throws FileNotFoundException {
		StandardBusinessDocument doc = new CreateSBD().createSBD(new Organisasjonsnummer("123456"), new Organisasjonsnummer("123456"), new Payload(
				createAsice().getBytes(), "UTF-8", "texkt/xml"));
		MarshalSBD.marshal(doc, System.out);
	}
}
