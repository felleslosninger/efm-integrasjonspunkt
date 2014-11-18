package no.difi.meldingsutveksling.dokumentpakking.domain;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class KeyStoreInfo {
	public final KeyStore keystore;
	public final String alias;
	public final String password;

	public KeyStoreInfo(final KeyStore keystore, final String alias, final String password) {
		this.keystore = keystore;
		this.alias = alias;
		this.password = password;
	}

	public PrivateKey getPrivateKey() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
		return (PrivateKey) keystore.getKey(alias, password.toCharArray());
	}

	public X509Certificate getCertificate() throws KeyStoreException {
		return (X509Certificate) keystore.getCertificate(alias);
	}

	public Certificate[] getCertificateChain() throws KeyStoreException {
		return keystore.getCertificateChain(alias);
	}
}