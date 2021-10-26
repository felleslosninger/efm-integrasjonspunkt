package no.difi.meldingsutveksling.dpi.client.internal;

import no.difi.certvalidator.Validator;
import no.difi.certvalidator.api.CertificateValidationException;
import no.difi.meldingsutveksling.dpi.client.internal.domain.Mode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;

class BusinessCertificateValidatorTest {

    private X509Certificate certificate;
    private BusinessCertificateValidator businessCertificateValidator;

    @BeforeEach
    public void beforeEach() throws CertificateValidationException {
        certificate = Validator.getCertificate(getClass().getResourceAsStream("/bc-test-digdir.cer"));
        businessCertificateValidator = BusinessCertificateValidator.of(Mode.TEST);
    }

    @Test
    void simpleTest() throws Exception {
        businessCertificateValidator.validate(certificate);
        businessCertificateValidator.validate(certificate.getEncoded());
        businessCertificateValidator.validate(getClass().getResourceAsStream("/bc-test-digdir.cer"));
    }

    @Test()
    void receiptNotFound() {
        Assertions.assertThrows(BusinessCertificateValidator.LoadingException.class, () -> BusinessCertificateValidator.of("/invalid-path.xml"));
    }
}