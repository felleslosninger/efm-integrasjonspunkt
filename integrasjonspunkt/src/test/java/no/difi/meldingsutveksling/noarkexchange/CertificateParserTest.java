package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.security.cert.Certificate;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class CertificateParserTest {

    @Test
    public void parseCertificate() {
        try {
            String certificate = TestConstants.certificate;
            final Certificate cert = CertificateParser.parse(certificate);
            MatcherAssert.assertThat(cert.getType(), is("X.509"));
        } catch (CertificateParserException e) {
            fail("Failed to parse certificate");
        }
    }
}