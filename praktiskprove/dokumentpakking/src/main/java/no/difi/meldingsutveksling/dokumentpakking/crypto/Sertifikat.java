/**
 * Copyright (C) Posten Norge AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.difi.meldingsutveksling.dokumentpakking.crypto;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.*;

public final class Sertifikat {

    private X509Certificate x509Certificate;

    private Sertifikat(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public byte[] getEncoded() {
        try {
            return x509Certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new RuntimeException("Kunne ikke hente encoded utgave av sertifikatet", e);
        }
    }

    public static Sertifikat fraBase64X509String(String base64) {
        try {
            return lagSertifikat(Base64.decodeBase64(base64));
        } catch (CertificateException e) {
            throw new RuntimeException("Kunne ikke lese sertifikat fra base64-streng", e);
        }
    }

    public static Sertifikat fraByteArray(byte[] certificate) {
        try {
            return lagSertifikat(certificate);
        } catch (CertificateException e) {
            throw new RuntimeException("Kunne ikke lese sertifikat fra byte array", e);
        }
    }

    public static Sertifikat fraCertificate(X509Certificate certificate) {
        return new Sertifikat(certificate);
    }

    public static Sertifikat fraKeyStore(KeyStore keyStore, String alias) {
        Certificate certificate;
        try {
            certificate = keyStore.getCertificate(alias);
        } catch (KeyStoreException e) {
            throw new RuntimeException("Klarte ikke lese sertifikat fra keystore", e);
        }

        if (certificate == null) {
            throw new RuntimeException("Kunne ikke finne sertifikat i keystore. Er du sikker på at det er brukt keystore med et sertifikat og at du har oppgitt riktig alias?");
        }

        if (!(certificate instanceof X509Certificate)) {
            throw new RuntimeException("Klienten støtter kun X509-sertifikater. Fikk sertifikat av typen " + certificate.getClass().getSimpleName());
        }

        return new Sertifikat((X509Certificate) certificate);
    }

    private static Sertifikat lagSertifikat(byte[] certificate) throws CertificateException {
        X509Certificate x509Certificate = (X509Certificate) CertificateFactory
                .getInstance("X509")
                .generateCertificate(new ByteArrayInputStream(certificate));
        return new Sertifikat(x509Certificate);
    }
}
