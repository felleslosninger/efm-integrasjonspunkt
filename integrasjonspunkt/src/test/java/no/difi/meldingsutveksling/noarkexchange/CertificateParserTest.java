package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import org.junit.Test;

import java.security.cert.Certificate;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class CertificateParserTest {
    private String certificate = TestConstants.certificate;

    @Test
    public void parseCertificate() {
        CertificateParser parser = new CertificateParser();

        try {
            final Certificate cert = parser.parse(certificate);
            assertThat(cert.getType(), is("X.509"));
        } catch (CertificateParserException e) {
            fail("Failed to parse certificate");
        }
    }
}