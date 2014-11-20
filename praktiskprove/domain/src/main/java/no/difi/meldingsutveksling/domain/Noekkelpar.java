package no.difi.meldingsutveksling.domain;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class Noekkelpar {

	private PrivateKey privatNoekkel;
	private Certificate sertifikat;

	public Noekkelpar(PrivateKey privatNoekkel, Certificate sertifikat) {
		this.privatNoekkel = privatNoekkel;
		this.sertifikat = sertifikat;
	}

	public Sertifikat getSertifikat() {
		return Sertifikat.fraCertificate((X509Certificate) sertifikat);
	}

	public Certificate[] getCertificateChain() {
		Certificate[] certs = new Certificate[1];
		certs[0] = sertifikat;
		return certs;
	}

	public PrivateKey getPrivateKey() {
		return privatNoekkel;
	}
}
