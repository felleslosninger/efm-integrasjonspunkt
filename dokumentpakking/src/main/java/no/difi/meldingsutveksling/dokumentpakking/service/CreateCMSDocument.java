package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
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

    //    private final Plumber plumber;
//    private final InMemoryWithTempFileFallbackResourceFactory resourceFactory;
    private final ASN1ObjectIdentifier cmsEncryptionAlgorithm;

//    public byte[] toByteArray(Input input) {
//        WritableByteArrayResource output = new WritableByteArrayResource();
//        createCMS(input, output);
//        return output.toByteArray();
//    }
//
//    public InMemoryWithTempFileFallbackResource createCMS(Input input) {
//        InMemoryWithTempFileFallbackResource output = resourceFactory.getResource(input.getTempFilePrefix(), ".cms");
//        createCMS(input, output);
//        return output;
//    }
//
//    public InputStreamResource createCMS(Input input, Reject reject) {
//        return new InputStreamResource(plumber.pipe("Creating CMS document", inlet -> {
//            try {
//                createCMS(input, new OutputStreamResource(inlet));
//            } catch (Exception e) {
//                reject.reject(new IllegalStateException("Couldn't create CMS document", e));
//            }
//        }, reject).outlet());
//    }

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
