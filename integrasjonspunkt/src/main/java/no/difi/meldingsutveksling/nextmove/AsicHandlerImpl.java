package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Streams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.SignatureMethod;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.dokumentpakking.domain.AsicEAttachable;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.dokumentpakking.domain.Manifest;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsAlgorithm;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSEncryptedAsice;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.pipe.Reject;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsicHandlerImpl implements AsicHandler {

    private final KeystoreHelper keystoreHelper;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final Adresseregister adresseregister;
    private final CreateCMSEncryptedAsice createCMSEncryptedAsice;
    private final ManifestFactory manifestFactory;

    @Override
    public void createEncryptedAsic(@NotNull NextMoveMessage msg, @NotNull WritableResource writableResource) {
        List<AsicEAttachable> documents = getDocuments(msg);
        AsicEAttachable mainDocument = documents.get(0);
        CreateCMSEncryptedAsice.Input input = getInput(msg, manifestFactory.createManifest(msg, mainDocument), documents.stream(), getMottakerSertifikat(msg));
        createCMSEncryptedAsice.createCmsEncryptedAsice(input, writableResource);
    }

    @NotNull
    @Override
    public Resource createEncryptedAsic(@NotNull NextMoveMessage msg, @NotNull Reject reject) {
        List<AsicEAttachable> documents = getDocuments(msg);
        AsicEAttachable mainDocument = documents.get(0);
        CreateCMSEncryptedAsice.Input input = getInput(msg, manifestFactory.createManifest(msg, mainDocument), documents.stream(), getMottakerSertifikat(msg));
        return createCMSEncryptedAsice.createCmsEncryptedAsice(input, reject);
    }

    @Override
    public Resource createEncryptedAsic(@NotNull NextMoveMessage msg, @NotNull Document mainDocument, @NotNull Stream<Document> attachments, @NotNull X509Certificate certificate, @NotNull Reject reject) {
        CreateCMSEncryptedAsice.Input input = getInput(msg, manifestFactory.createManifest(msg, mainDocument), Streams.concat(Stream.of(mainDocument), attachments), certificate);
        return createCMSEncryptedAsice.createCmsEncryptedAsice(input, reject);
    }

    private CreateCMSEncryptedAsice.Input getInput(@NotNull NextMoveMessage msg, @NotNull Manifest manifest, @NotNull Stream<AsicEAttachable> documents, @NotNull X509Certificate certificate) {
        return CreateCMSEncryptedAsice.Input.builder()
                .manifest(manifest)
                .documents(documents)
                .certificate(certificate)
                .signatureMethod(SignatureMethod.CAdES)
                .signatureHelper(keystoreHelper.getSignatureHelper())
                .keyEncryptionScheme(msg.getServiceIdentifier() == DPE ? null : CmsAlgorithm.RSAES_OAEP)
                .tempFilePrefix(msg.getSenderIdentifier().toLowerCase() + "-")
                .build();
    }

    private List<AsicEAttachable> getDocuments(NextMoveMessage msg) {
        return msg.getFiles().stream()
                .sorted((a, b) -> {
                    if (Boolean.TRUE.equals(a.getPrimaryDocument())) return -1;
                    if (Boolean.TRUE.equals(b.getPrimaryDocument())) return 1;
                    return a.getDokumentnummer().compareTo(b.getDokumentnummer());
                }).map(f -> Document.builder()
                        .filename(f.getFilename())
                        .mimeType(getMimetype(f))
                        .title(f.getTitle())
                        .resource(getResource(msg, f))
                        .build()).collect(Collectors.toList());
    }

    private Resource getResource(NextMoveMessage msg, BusinessMessageFile f) {
        try {
            return optionalCryptoMessagePersister.read(msg.getMessageId(), f.getIdentifier());
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Could not read file messageId=%s, filename=%s", msg.getMessageId(), f.getIdentifier()), e);
        }
    }

    private String getMimetype(BusinessMessageFile f) {
        if (StringUtils.hasText(f.getMimetype())) {
            return f.getMimetype();
        }

        String ext = Stream.of(f.getFilename().split("\\.")).reduce((p, e) -> e).orElse("pdf");
        return MimeTypeExtensionMapper.getMimetype(ext);
    }

    private X509Certificate getMottakerSertifikat(NextMoveMessage message) {
        return (X509Certificate) adresseregister.getReceiverCertificate(message);
    }


}
