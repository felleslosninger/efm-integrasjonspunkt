package no.difi.meldingsutveksling.noarkexchange;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.StringReader;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class CertificateParser {

    private final CertificateFactory certificateFactory;

    public CertificateParser() {
        certificateFactory = new CertificateFactory();
    }

    public Certificate parse(String certificate) throws IOException, CertificateException {
        PEMParser pemParser = new PEMParser(new StringReader(certificate));
        final X509CertificateHolder holder  = (X509CertificateHolder) pemParser.readObject();
        final JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
        return jcaX509CertificateConverter.getCertificate(holder);
    }
}
