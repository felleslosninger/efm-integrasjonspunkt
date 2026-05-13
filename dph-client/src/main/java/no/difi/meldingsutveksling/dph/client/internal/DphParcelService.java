package no.difi.meldingsutveksling.dph.client.internal;

import lombok.RequiredArgsConstructor;
import no.difi.asic.SignatureMethod;
import no.difi.meldingsutveksling.dph.client.DigdirBusinessCertificateSupplier;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.dokumentpakking.CmsAlgorithm;
import no.difi.move.common.dokumentpakking.CreateCMSEncryptedAsice;
import no.difi.move.common.dokumentpakking.JavaWebEncryption;
import no.difi.move.common.dokumentpakking.JavaWebToken;
import no.difi.move.common.dokumentpakking.domain.AsicEAttachable;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import no.difi.move.common.io.ResourceUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.multipart.Part;

import java.util.stream.Stream;

@RequiredArgsConstructor
public class DphParcelService {

    private final KeystoreHelper keystoreHelper;
    private final CreateCMSEncryptedAsice createCmsEncryptedAsice;
    private final DigdirBusinessCertificateSupplier digdirBusinessCertificateSupplier;
    private final InMemoryWithTempFileFallbackResourceFactory resourceFactory;

    public String signAndEncrypt(String payload) {
        String signed = JavaWebToken.sign(payload, keystoreHelper.loadPrivateKey());
        return JavaWebEncryption.encrypt(signed, digdirBusinessCertificateSupplier.get());
    }

    public String decryptAndVerify(String jweToken) {
        return verify(decrypt(jweToken));
    }

    public String verify(String signed) {
        return JavaWebToken.verify(signed, digdirBusinessCertificateSupplier.get());
    }

    public String decrypt(String jweToken) {
        return JavaWebEncryption.decrypt(jweToken, keystoreHelper.loadPrivateKey());
    }

    public InMemoryWithTempFileFallbackResource createAndEncryptAsic(Stream<? extends AsicEAttachable> attachments) {
        InMemoryWithTempFileFallbackResource resource = resourceFactory.getResource("dph-", ".asic.cms");
        createCmsEncryptedAsice.createCmsEncryptedAsice(
            CreateCMSEncryptedAsice.Input.builder()
                .documents(attachments)
                .certificate(digdirBusinessCertificateSupplier.get())
                .signatureMethod(SignatureMethod.CAdES)
                .signatureHelper(keystoreHelper.getSignatureHelper())
                .keyEncryptionScheme(CmsAlgorithm.RSAES_OAEP)
                .build(),
            resource
        );

        return resource;
    }

    public Resource getEncryptedAsic(Part part) {
        InMemoryWithTempFileFallbackResource resource = resourceFactory.getResource("dph-", ".asic.cms");
        ResourceUtils.copy(part.content(), resource);
        return resource;
    }
}
