package no.difi.meldingsutveksling.nextmove;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Getter
@RequiredArgsConstructor
public class EncryptedBusinessMessage implements BusinessMessage {

    public static final CertificateFactory cf;

    static {
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException("Not able to initialize X.509 certificate factory",e);
        }
    }

    private final String base64DerEncryptionCertificate;
    private final String message;

    public X509Certificate getX509Certificate() throws CertificateException {
        return (X509Certificate)cf.generateCertificate(new ByteArrayInputStream( Base64.getDecoder().decode(base64DerEncryptionCertificate.getBytes(StandardCharsets.UTF_8))));
    }

}
