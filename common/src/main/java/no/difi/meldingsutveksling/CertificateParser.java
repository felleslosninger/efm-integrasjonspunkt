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

    public X509Certificate parse(String certificate) throws CertificateParserException {
        return parse(new StringReader(certificate));

    }

    public X509Certificate parse(Reader reader) throws CertificateParserException {
        PEMParser pemParser = new PEMParser(reader);

        final X509CertificateHolder holder;
        try {
            holder = (X509CertificateHolder) pemParser.readObject();
        } catch (IOException e) {
            throw new CertificateParserException("Failed to read certificate from PEMParser", e);
        }
        final JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
        try {
            return jcaX509CertificateConverter.getCertificate(holder);
        } catch (CertificateException e) {
            throw new CertificateParserException("Failed to convert certificate to X509", e);
        }
    }
}
