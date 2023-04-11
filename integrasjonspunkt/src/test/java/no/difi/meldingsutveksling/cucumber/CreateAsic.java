package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import no.difi.asic.SignatureMethod;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSEncryptedAsice;
import no.difi.meldingsutveksling.nextmove.ManifestFactory;
import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.pipe.Reject;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class CreateAsic {

    private final KeystoreHelper keystoreHelper;
    private final ManifestFactory manifestFactory;
    private final CreateCMSEncryptedAsice createCMSEncryptedAsice;
    private final Supplier<AlgorithmIdentifier> algorithmIdentifierSupplier;

    Resource createAsic(Message message, Reject reject) {
        return createCMSEncryptedAsice.createCmsEncryptedAsice(getInput(message), reject);
    }

    void createAsic(Message message, WritableResource writableResource) {
        createCMSEncryptedAsice.createCmsEncryptedAsice(getInput(message), writableResource);
    }

    private CreateCMSEncryptedAsice.Input getInput(Message message) {
        return CreateCMSEncryptedAsice.Input.builder()
                .manifest(manifestFactory.createManifest(NextMoveInMessage.of(message.getSbd(), message.getServiceIdentifier()),
                        message.getAttachments().get(0)))
                .documents(message.getAttachments().stream())
                .certificate(keystoreHelper.getX509Certificate())
                .signatureMethod(SignatureMethod.CAdES)
                .signatureHelper(keystoreHelper.getSignatureHelper())
                .keyEncryptionScheme(algorithmIdentifierSupplier.get())
                .build();
    }
}
