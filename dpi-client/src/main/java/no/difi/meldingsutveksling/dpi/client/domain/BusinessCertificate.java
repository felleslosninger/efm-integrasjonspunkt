package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Value;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Value
public class BusinessCertificate {

    X509Certificate x509Certificate;

    public PublicKey getPublicKey() {
        return x509Certificate.getPublicKey();
    }

    public byte[] getEncoded() {
        try {
            return x509Certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new Exception("Kunne ikke hente encoded utgave av sertifikatet", e);
        }
    }

    public static BusinessCertificate of(byte[] certificate) {
        try {
            return createBusinessCertificate(certificate);
        } catch (CertificateException e) {
            throw new Exception("Kunne ikke lese sertifikat fra byte array", e);
        }
    }

    public static BusinessCertificate getFromKeyStore(KeyStore keyStore, String alias) {
        java.security.cert.Certificate certificate;
        try {
            certificate = keyStore.getCertificate(alias);
        } catch (KeyStoreException e) {
            throw new Exception("Klarte ikke lese sertifikat fra keystore", e);
        }

        if (certificate == null) {
            throw new Exception("Kunne ikke finne sertifikat i keystore. Er du sikker på at det er brukt keystore med et sertifikat og at du har oppgitt riktig alias?");
        }

        if (!(certificate instanceof X509Certificate)) {
            throw new Exception("Klienten støtter kun X509-sertifikater. Fikk sertifikat av typen " + certificate.getClass().getSimpleName());
        }

        return new BusinessCertificate((X509Certificate) certificate);
    }

    private static BusinessCertificate createBusinessCertificate(byte[] certificate) throws CertificateException {
        X509Certificate x509Certificate = (X509Certificate) CertificateFactory
                .getInstance("X509")
                .generateCertificate(new ByteArrayInputStream(certificate));
        return new BusinessCertificate(x509Certificate);
    }

    private static class Exception extends RuntimeException {
        public Exception(String message) {
            super(message);
        }

        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
