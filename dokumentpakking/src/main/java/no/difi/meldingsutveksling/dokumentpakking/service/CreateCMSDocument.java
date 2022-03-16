package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.difi.move.common.io.OutputStreamResource;
import no.difi.move.common.io.pipe.Pipe;
import no.difi.move.common.io.pipe.PipeResource;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.Reject;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSEnvelopedDataStreamGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OutputEncryptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

@RequiredArgsConstructor
public class CreateCMSDocument {

    private final Plumber plumber;
    private final ASN1ObjectIdentifier cmsEncryptionAlgorithm;

    public Resource encrypt(Input input, Reject reject) {
        Pipe pipe = plumber.pipe("Encrypting",
                inlet -> encrypt(input, new OutputStreamResource(inlet)),
                reject);

        return new PipeResource(pipe, String.format("Encrypted %s", input.getResource().getDescription()));
    }

    public void encrypt(Input input, WritableResource output) {
        try {
            JceKeyTransRecipientInfoGenerator recipientInfoGenerator = new JceKeyTransRecipientInfoGenerator(
                    input.getCertificate(), input.getKeyEncryptionScheme())
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);

            CMSEnvelopedDataGenerator envelopedDataGenerator = new CMSEnvelopedDataGenerator();
            envelopedDataGenerator.addRecipientInfoGenerator(recipientInfoGenerator);

            CMSEnvelopedDataStreamGenerator cmsEnvelopedDataStreamGenerator = new CMSEnvelopedDataStreamGenerator();
            cmsEnvelopedDataStreamGenerator.addRecipientInfoGenerator(recipientInfoGenerator);

            OutputEncryptor contentEncryptor = new JceCMSContentEncryptorBuilder(cmsEncryptionAlgorithm).build();
            try (OutputStream open = cmsEnvelopedDataStreamGenerator.open(output.getOutputStream(), contentEncryptor)) {
                try (InputStream inputStream = input.getResource().getInputStream()) {
                    StreamUtils.copy(inputStream, open);
                }
            }
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException("Something is wrong with the certificate", e);
        } catch (CMSException | IOException e) {
            throw new IllegalStateException("Couldn't create Cryptographic Message Syntax for document package!", e);
        }
    }

    @Value
    @Builder
    public static class Input {
        @NonNull Resource resource;
        @NonNull X509Certificate certificate;
        AlgorithmIdentifier keyEncryptionScheme;
        @Builder.Default
        String tempFilePrefix = "";
    }
}
