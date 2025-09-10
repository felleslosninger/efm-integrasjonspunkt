package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.cert.Certificate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CertificateParserTest {

    @Test
    public void parseCertificate() {
        try {
            String certificate = TestConstants.certificate;
            final Certificate cert = CertificateParser.parse(certificate);
            assertThat(cert.getType(), is("X.509"));
        } catch (CertificateParserException e) {
            Assertions.fail("Failed to parse certificate");
        }
    }
}