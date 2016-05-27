package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.Certificate;

public class StringConvertingCertificateWrapper {

    public static String toString(Certificate certificate) {
        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        try {
            jcaPEMWriter = new JcaPEMWriter(stringWriter);
            jcaPEMWriter.writeObject(certificate);
            jcaPEMWriter.flush();
            return stringWriter.toString();
        } catch (IOException e) {
            throw new ServiceDiscoveryException(e);
        } finally {
            IOUtils.closeQuietly(jcaPEMWriter);
            IOUtils.closeQuietly(stringWriter);
        }
    }
}