package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSEnvelopedDataStreamGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OutputEncryptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

@RequiredArgsConstructor
public class CreateCMSDocument {

    private final ASN1ObjectIdentifier cmsEncryptionAlgorithm;
    private final AlgorithmIdentifier keyEncryptionScheme;

    public void createCMS(InputStream inputStream, OutputStream outputStream, X509Certificate businessCertificate) {
        try {
            JceKeyTransRecipientInfoGenerator recipientInfoGenerator = new JceKeyTransRecipientInfoGenerator(businessCertificate, keyEncryptionScheme)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);

            CMSEnvelopedDataGenerator envelopedDataGenerator = new CMSEnvelopedDataGenerator();
            envelopedDataGenerator.addRecipientInfoGenerator(recipientInfoGenerator);

            CMSEnvelopedDataStreamGenerator cmsEnvelopedDataStreamGenerator = new CMSEnvelopedDataStreamGenerator();
            cmsEnvelopedDataStreamGenerator.addRecipientInfoGenerator(recipientInfoGenerator);

            OutputEncryptor contentEncryptor = new JceCMSContentEncryptorBuilder(cmsEncryptionAlgorithm).build();
            OutputStream open = cmsEnvelopedDataStreamGenerator.open(outputStream, contentEncryptor);
            IOUtils.copyLarge(inputStream, open);
            open.close();
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException("Something is wrong with the business certificate", e);
        } catch (CMSException | IOException e) {
            throw new IllegalStateException("Couldn't create Cryptographic Message Syntax for document package!", e);
        }
    }
}
