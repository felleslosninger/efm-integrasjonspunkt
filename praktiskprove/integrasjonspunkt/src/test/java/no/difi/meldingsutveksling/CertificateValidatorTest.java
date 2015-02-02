package no.difi.meldingsutveksling;

import org.junit.Test;

import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * This test makes sure that the "integrasjonspunkt" can instantiate the KeystoreUtil class and verify
 * a validated certificate and its chain
 */
public class CertificateValidatorTest {


    @Test
    public void testValidCertificate() throws CertificateException {

        CertificateValidator validator = new CertificateValidator();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("974720760.pem");
        X509Certificate certificate = (X509Certificate) cf.generateCertificate(is);
        validator.validate(certificate);

    }


}
