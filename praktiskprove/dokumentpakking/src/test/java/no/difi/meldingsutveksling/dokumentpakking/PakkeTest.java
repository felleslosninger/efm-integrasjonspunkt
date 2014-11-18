package no.difi.meldingsutveksling.dokumentpakking;

import java.io.File;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import no.difi.meldingsutveksling.adresseregmock.AdressRegisterFactory;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

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

	@Test
	public void testPakkingAvXML() throws IOException, InvalidKeySpecException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(avsenderPrivateKey));
		KeyFactory kf = KeyFactory.getInstance("RSA");

		Avsender avsender = Avsender
				.builder(
						new Organisasjonsnummer("960885406"),
						new Noekkelpar(kf.generatePrivate(keySpec),
								(Certificate) AdressRegisterFactory.createAdressRegister().getCertificate("960885406"))).build();
		Mottaker mottaker = new Mottaker(new Organisasjonsnummer("958935429"), mottakerpublicKey);

		datapakker.pakkDokumentISbd(forsendelse, avsender, mottaker);
	}
}
