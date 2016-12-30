package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.CertificateParser;
import org.junit.Test;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class CertificateParserTest {
    private String certificate = TestConstants.certificate;

    @Test
    public void parseCertificate() {
        CertificateParser parser = new CertificateParser();

        try {
            final Certificate cert = parser.parse(certificate);
            assertThat(cert.getType(), is("X.509"));
        } catch (CertificateException e) {
            fail("Failed to parse certificate");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}