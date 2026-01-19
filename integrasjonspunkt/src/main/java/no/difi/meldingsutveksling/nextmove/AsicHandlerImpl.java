package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.SignatureMethod;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsAlgorithm;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSEncryptedAsice;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.pipe.Reject;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Stream;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;

@Slf4j
@Component
public class AsicHandlerImpl implements AsicHandler {

    private final KeystoreHelper keystoreHelper;
    private final Adresseregister adresseregister;
    private final CreateCMSEncryptedAsice createCMSEncryptedAsice;
    private final ManifestFactory manifestFactory;
    private final NextMoveMessageService nextMoveMessageService;

    public AsicHandlerImpl(KeystoreHelper keystoreHelper,
                           Adresseregister adresseregister,
                           CreateCMSEncryptedAsice createCMSEncryptedAsice,
                           ManifestFactory manifestFactory,
                           @Lazy NextMoveMessageService nextMoveMessageService) {
        this.keystoreHelper = keystoreHelper;
        this.adresseregister = adresseregister;
        this.createCMSEncryptedAsice = createCMSEncryptedAsice;
        this.manifestFactory = manifestFactory;
        this.nextMoveMessageService = nextMoveMessageService;
    }

    @Override
    public void createCmsEncryptedAsice(@NotNull NextMoveMessage msg, @NotNull WritableResource writableResource) {
        List<Document> documents = nextMoveMessageService.getDocuments(msg);
        createCMSEncryptedAsice.createCmsEncryptedAsice(getInputBuilder(msg, documents.getFirst())
                .documents(documents.stream())
                .certificate(getMottakerSertifikat(msg))
                .build(), writableResource);
    }

    @NotNull
    @Override
    public Resource createCmsEncryptedAsice(@NotNull NextMoveMessage msg, @NotNull Reject reject) {
        List<Document> documents = nextMoveMessageService.getDocuments(msg);
        return createCMSEncryptedAsice.createCmsEncryptedAsice(getInputBuilder(msg, documents.getFirst())
                .documents(documents.stream())
                .certificate(getMottakerSertifikat(msg))
                .build(), reject);
    }

    @NotNull
    @Override
    public Resource createCmsEncryptedAsice(@NotNull NextMoveMessage msg, @NotNull Document mainDocument, @NotNull Stream<Document> attachments, @NotNull X509Certificate certificate, @NotNull Reject reject) {
        return createCMSEncryptedAsice.createCmsEncryptedAsice(getInputBuilder(msg, mainDocument)
                .documents(Streams.concat(Stream.of(mainDocument), attachments))
                .certificate(certificate)
                .build(), reject);
    }

    private CreateCMSEncryptedAsice.Input.InputBuilder getInputBuilder(@NotNull NextMoveMessage msg, @NotNull AsicEAttachable mainDocument) {
        return CreateCMSEncryptedAsice.Input.builder()
                .manifest(manifestFactory.createManifest(msg, mainDocument))
                .signatureMethod(SignatureMethod.CAdES)
                .signatureHelper(keystoreHelper.getSignatureHelper())
                .keyEncryptionScheme(msg.getServiceIdentifier() == DPE ? null : CmsAlgorithm.RSAES_OAEP);
    }

    private X509Certificate getMottakerSertifikat(NextMoveMessage message) {
        return (X509Certificate) adresseregister.getReceiverCertificate(message);
    }
}
