package no.difi.meldingsutveksling;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertificateParser {

    public CertificateParser() {
    }

    public X509Certificate parse(String certificate) throws IOException, CertificateException {
        return parse(new StringReader(certificate));

    }

    public X509Certificate parse(Reader reader) throws CertificateException, IOException {
        PEMParser pemParser = new PEMParser(reader);

        final X509CertificateHolder holder  = (X509CertificateHolder) pemParser.readObject();
        final JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
        return jcaX509CertificateConverter.getCertificate(holder);
    }
}
