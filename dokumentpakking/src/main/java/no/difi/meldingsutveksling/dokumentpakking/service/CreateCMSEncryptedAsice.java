package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.SignatureHelper;
import no.difi.asic.SignatureMethod;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Manifest;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import no.difi.move.common.io.OutputStreamResource;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.security.cert.X509Certificate;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class CreateCMSEncryptedAsice {

    private final Plumber plumber;
    private final PromiseMaker promiseMaker;
    private final CreateAsice createASiCE;
    private final CreateCMSDocument createCMS;

    public byte[] toByteArray(Input input) {
        byte[] asic = createASiCE.toByteArray(input.getCreateAsicInput());
        return createCMS.toByteArray(input.getCreateCMSDocumentInput(new ByteArrayResource(asic)));
    }

    public InMemoryWithTempFileFallbackResource createCmsEncryptedAsice(Input input) {
        try (InMemoryWithTempFileFallbackResource asic = createASiCE.createAsice(input.getCreateAsicInput())) {
            return createCMS.createCMS(input.getCreateCMSDocumentInput(asic));
        } catch (Exception e) {
            throw new IllegalStateException("Could not create CMS Encrypted Asice", e);
        }
    }

    public InputStreamResource createCmsEncryptedAsice(Input input, Reject reject) {
        return new InputStreamResource(plumber.pipe("Creating CMS encrypted Asice", inlet -> {
            try {
                createCmsEncryptedAsice(input, new OutputStreamResource(inlet));
            } catch (Exception e) {
                reject.reject(e);
            }
        }, reject).outlet());
    }

    public void createCmsEncryptedAsice(Input input, WritableResource output) {
        promiseMaker.promise(reject -> {
            InputStreamResource asic = createASiCE.createAsice(input.getCreateAsicInput(), reject);
            try {
                createCMS.createCMS(input.getCreateCMSDocumentInput(asic), output);
            } catch (Exception e) {
                reject.reject(e);
            }
            return null;
        });
    }

    @Value
    @Builder
    public static class Input {
        @NonNull Manifest manifest;
        @NonNull Stream<AsicEAttachable> documents;
        @NonNull X509Certificate certificate;
        @NonNull SignatureMethod signatureMethod;
        @NonNull SignatureHelper signatureHelper;
        AlgorithmIdentifier keyEncryptionScheme;
        @Builder.Default String tempFilePrefix = "";

        private CreateAsice.Input getCreateAsicInput() {
            return CreateAsice.Input.builder()
                    .documents(documents)
                    .manifest(manifest)
                    .certificate(certificate)
                    .signatureMethod(signatureMethod)
                    .signatureHelper(signatureHelper)
                    .tempFilePrefix(tempFilePrefix)
                    .build();
        }

        public CreateCMSDocument.Input getCreateCMSDocumentInput(Resource resource) {
            return CreateCMSDocument.Input.builder()
                    .resource(resource)
                    .certificate(certificate)
                    .keyEncryptionScheme(keyEncryptionScheme)
                    .tempFilePrefix(tempFilePrefix)
                    .build();
        }
    }
}
