package no.difi.meldingsutveksling.dph.client.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import lombok.RequiredArgsConstructor;
import no.difi.asic.SignatureMethod;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dph.client.DigdirBusinessCertificateSupplier;
import no.difi.meldingsutveksling.dph.client.DphException;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.dokumentpakking.CmsAlgorithm;
import no.difi.move.common.dokumentpakking.CreateCMSEncryptedAsice;
import no.difi.move.common.dokumentpakking.CreateSignedJWT;
import no.difi.move.common.dokumentpakking.JsonWebEncryption;
import no.difi.move.common.dokumentpakking.VerifyJWT;
import no.difi.move.common.dokumentpakking.domain.AsicEAttachable;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import no.difi.move.common.io.ResourceUtils;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;
import org.bouncycastle.cms.CMSAlgorithm;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.multipart.Part;

import java.util.stream.Stream;

@RequiredArgsConstructor
public class DphParcelService {

    private final VerifyJWT verifyJWT;
    private final ObjectMapper objectMapper;
    private final KeystoreHelper keystoreHelper;
    private final CreateCMSEncryptedAsice createCmsEncryptedAsice;
    private final DigdirBusinessCertificateSupplier digdirBusinessCertificateSupplier;
    private final InMemoryWithTempFileFallbackResourceFactory resourceFactory;

    public String toJSON(StandardBusinessDocument sbd) {
        try {
            return objectMapper.writeValueAsString(sbd);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Serializing SBD to JSON failed", e);
        }
    }

    public StandardBusinessDocument toSBD(String json) {
        try {
            return objectMapper.readValue(json, StandardBusinessDocument.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Deserializing from JSON to SBD failed", e);
        }
    }

    public String signAndEncrypt(String payload) {
        String signed = CreateSignedJWT.createSignedJWT(CreateSignedJWT.Input.builder()
            .payload(payload)
            .privateKey(keystoreHelper.loadPrivateKey())
            .algorithm(JWSAlgorithm.PS256)
            .certificate(keystoreHelper.getX509Certificate())
            .build());
        return JsonWebEncryption.encrypt(signed, digdirBusinessCertificateSupplier.get());
    }

    public JWSObject decryptAndVerify(String jweToken) {
        return verify(decrypt(jweToken));
    }

    public JWSObject verify(String signed) {
        try {
            return verifyJWT.verify(signed);
        } catch (Exception e) {
            throw new DphException(FeilmeldingForApplikasjonskvittering.SIGNATURFEIL);
        }
    }

    public String decrypt(String jweToken) {
        try {
            return JsonWebEncryption.decrypt(jweToken, keystoreHelper.loadPrivateKey());
        } catch (Exception e) {
            throw new DphException(FeilmeldingForApplikasjonskvittering.UGYLIG_SERTIFIKAT);
        }
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
                .cmsEncryptionAlgorithm(CMSAlgorithm.AES256_GCM)
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
