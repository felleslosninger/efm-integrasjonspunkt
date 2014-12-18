package no.difi.meldingsutveksling.dokumentpakking;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import no.difi.meldingsutveksling.adresseregister.AdressRegisterFactory;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Node;

import static org.mockito.Mockito.mock;

public class PakkeTest {
	final PublicKey mottakerpublicKey = AdressRegisterFactory.createAdressRegister().getPublicKey("958935429");
	final String avsenderPrivateKey = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAM7w0IG4Cj7Pr7KH"
			+ "DD4fM3LFlzvN5Pju2bnNnsDRhoR7wsK+xxVXLcsl+kScNxNIjy2+6BaR+pniM4bA" + "TqK1fjrN2oEZ6MinITHJzuQYp/MTg4+afCV4vKXmkl+siopjjwWWD7a4FhP6TQfj"
			+ "gcApPIEf1iwo8bghL2tQGwUjohLFAgMBAAECgYAling44Bszs9eKyocFCgH6UzAR" + "UFO2eRYUZ+Hh1uDRTeZSD+vryinrjZMuOSygmewnf1d5KLhOjEOOsXpSeBxS2RYo"
			+ "csteW78txCRsSEJo7i9ASmw7w0vvN0tVqTCbjNokI8xS6Kn+GH96vMCNq4ImuBkg" + "zWuaDA6GP/FQorSCzQJBAOvxwJIWvc44aSTceYYOVQUJZ3b9a6y2rpuvdcpuPwJL"
			+ "Y2YbPq5SlQAhsh5Yss2d8aAGvaDXbZOPVZyvCaeAMf8CQQDgh+4uNMQAu556DNPa" + "GDl3JI4JmgZl8bbiRQRFU5h02AkoNv03izyafOqpl61X1WCeHBx1nn2ivIXJ/0ub"
			+ "X3M7AkBshC3rguYdOLizKWwDCgh0XpTllzy0nPjFxfdI+VeleILo7VLw3i6Fdvnz" + "Fxx1kVUWIsOIfEx7d4sKmz63eTCFAkB+MafabGmlB84gRsljEK5rmi4Ck4D5Fwt0"
			+ "zNmDpWJQeYNcCNv0tdsP8RlqzAbvEMxG0QHl0XhHWLHRQB1cbB81AkEAtnzxewKS" + "P40rj3bkKSj8tuSOBbnpzWp93P8FFkyHNZCKbEArf89gYHLopwoe3kixp3u8QiXl"
			+ "s2TPH0mjyb7Keg==";
	private ByteArrayFile forsendelse = new ByteArrayFile() {
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

	@Test @Ignore
	public void testPakkingAvXML() throws IOException, InvalidKeySpecException, KeyStoreException, NoSuchAlgorithmException, CertificateException, CMSException, OperatorCreationException, ParseException, JAXBException {
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(avsenderPrivateKey));
		KeyFactory kf = KeyFactory.getInstance("RSA");
		
		Avsender avsender = Avsender.builder(new Organisasjonsnummer("960885406"),
				new Noekkelpar(kf.generatePrivate(keySpec), (Certificate) AdressRegisterFactory.createAdressRegister().getCertificate("960885406"))).build();
		Mottaker mottaker = new Mottaker(new Organisasjonsnummer("958935429"), (X509Certificate) AdressRegisterFactory.createAdressRegister().getCertificate(
				"958935429"));
		 assertThat(new Dokumentpakker().pakkDokumentIStandardBusinessDocument(forsendelse, avsender, mottaker, "123", "Melding"), is(notNullValue()));
		 
		 StandardBusinessDocument sbd = new Dokumentpakker().pakkDokumentIStandardBusinessDocument(forsendelse, avsender, mottaker, "123", "Melding");
//		 new CmsUtil().decryptCMS(Base64.decodeBase64(((Payload)sbd.getAny()).getContent()), null);
		 ByteArrayOutputStream os = new ByteArrayOutputStream();
		 MarshalSBD.marshal(sbd, os);
		 
		 Unmarshaller unmarshaller = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class).createUnmarshaller();
		 StandardBusinessDocument sbd2 = unmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(os.toByteArray())), StandardBusinessDocument.class).getValue();
		 new CmsUtil().decryptCMS(Base64.decodeBase64(((Payload)sbd2.getAny()).getContent()), null);

		 
		 
		 
	}
}
