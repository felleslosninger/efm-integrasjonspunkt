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
import no.difi.move.common.io.pipe.PromiseMaker;
import no.difi.move.common.io.pipe.Reject;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.security.cert.X509Certificate;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class CreateCMSEncryptedAsice {

    private final PromiseMaker promiseMaker;
    private final CreateAsice createASiCE;
    private final CreateCMSDocument createCMS;

    public Resource createCmsEncryptedAsice(Input input, Reject reject) {
        Resource asic = createASiCE.createAsice(input.getCreateAsicInput(), reject);
        return createCMS.encrypt(input.getCreateCMSDocumentInput(asic), reject);
    }

    public void createCmsEncryptedAsice(Input input, WritableResource output) {
        promiseMaker.promise(reject -> {
            Resource asic = createASiCE.createAsice(input.getCreateAsicInput(), reject);
            createCMS.encrypt(input.getCreateCMSDocumentInput(asic), output);
            return null;
        }).await();
    }

    @Value
    @Builder
    public static class Input {
        @NonNull Manifest manifest;
        @NonNull Stream<? extends AsicEAttachable> documents;
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
