package no.difi.meldingsutveksling.dokumentpakking;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.management.RuntimeErrorException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import no.difi.meldingsutveksling.adresseregmock.AdressRegisterFactory;
import no.difi.meldingsutveksling.dokumentpakking.crypto.Noekkelpar;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Avsender;
import no.difi.meldingsutveksling.dokumentpakking.domain.Mottaker;
import no.difi.meldingsutveksling.dokumentpakking.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.SBD;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Node;

public class DemoPakking {
	public SBD wrapContentInSPD(Object o) {
		Dokumentpakker dokumentpakker = new Dokumentpakker();
		final PublicKey mottakerpublicKey = AdressRegisterFactory.createAdressRegister().getPublicKey("958935429");
		final String avsenderPrivateKey = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAM7w0IG4Cj7Pr7KH"
				+ "DD4fM3LFlzvN5Pju2bnNnsDRhoR7wsK+xxVXLcsl+kScNxNIjy2+6BaR+pniM4bA" + "TqK1fjrN2oEZ6MinITHJzuQYp/MTg4+afCV4vKXmkl+siopjjwWWD7a4FhP6TQfj"
				+ "gcApPIEf1iwo8bghL2tQGwUjohLFAgMBAAECgYAling44Bszs9eKyocFCgH6UzAR" + "UFO2eRYUZ+Hh1uDRTeZSD+vryinrjZMuOSygmewnf1d5KLhOjEOOsXpSeBxS2RYo"
				+ "csteW78txCRsSEJo7i9ASmw7w0vvN0tVqTCbjNokI8xS6Kn+GH96vMCNq4ImuBkg" + "zWuaDA6GP/FQorSCzQJBAOvxwJIWvc44aSTceYYOVQUJZ3b9a6y2rpuvdcpuPwJL"
				+ "Y2YbPq5SlQAhsh5Yss2d8aAGvaDXbZOPVZyvCaeAMf8CQQDgh+4uNMQAu556DNPa" + "GDl3JI4JmgZl8bbiRQRFU5h02AkoNv03izyafOqpl61X1WCeHBx1nn2ivIXJ/0ub"
				+ "X3M7AkBshC3rguYdOLizKWwDCgh0XpTllzy0nPjFxfdI+VeleILo7VLw3i6Fdvnz" + "Fxx1kVUWIsOIfEx7d4sKmz63eTCFAkB+MafabGmlB84gRsljEK5rmi4Ck4D5Fwt0"
				+ "zNmDpWJQeYNcCNv0tdsP8RlqzAbvEMxG0QHl0XhHWLHRQB1cbB81AkEAtnzxewKS" + "P40rj3bkKSj8tuSOBbnpzWp93P8FFkyHNZCKbEArf89gYHLopwoe3kixp3u8QiXl"
				+ "s2TPH0mjyb7Keg==";

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(avsenderPrivateKey));
		KeyFactory kf = null;
		try {
			kf = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		Avsender avsender = null;
		try {
			avsender = Avsender.builder(
					new Organisasjonsnummer("960885406"),
					Noekkelpar.createNoekkelpar(kf.generatePrivate(keySpec),
							(Certificate) AdressRegisterFactory.createAdressRegister().getCertificate("960885406"))).build();
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException("Invalid key for avsender", e);
		}
		Mottaker mottaker = new Mottaker(new Organisasjonsnummer("958935429"), mottakerpublicKey);
		System.out.println(o.toString());

		return new SBD(dokumentpakker.pakkDokumentISbd(new AsiceContent(nodeToByteArray((Node) o)), avsender, mottaker));

	}

	private byte[] nodeToByteArray(Node node) {
		StringWriter sw = new StringWriter();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.transform(new DOMSource(node), new StreamResult(os));
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
		return os.toByteArray();
	}

	private class AsiceContent implements AsicEAttachable {
		private byte[] content;

		public AsiceContent(byte[] content) {
			this.content = content;
		}

		@Override
		public String getFileName() {
			// TODO Auto-generated method stub
			return "edu_best.xml";
		}

		@Override
		public byte[] getBytes() {
			// TODO Auto-generated method stub
			return content;
		}

		@Override
		public String getMimeType() {
			// TODO Auto-generated method stub
			return "text/xml";
		}

	}

}
