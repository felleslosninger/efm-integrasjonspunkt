package no.difi.meldingsutveksling.dpi.client;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.asic.SignatureMethod;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsAlgorithm;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSEncryptedAsice;
import no.difi.meldingsutveksling.dpi.client.domain.GetMessagesInput;
import no.difi.meldingsutveksling.dpi.client.domain.MessageStatus;
import no.difi.meldingsutveksling.dpi.client.domain.ReceivedMessage;
import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import no.difi.meldingsutveksling.dpi.client.internal.Corner2Client;
import no.difi.meldingsutveksling.dpi.client.internal.CreateManifest;
import no.difi.meldingsutveksling.dpi.client.internal.CreateSendMessageInput;
import no.difi.meldingsutveksling.dpi.client.internal.MessageUnwrapper;
import no.difi.meldingsutveksling.dpi.client.internal.domain.SendMessageInput;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class DpiClientImpl implements DpiClient {

    private final InMemoryWithTempFileFallbackResourceFactory resourceFactory;
    private final CreateCMSEncryptedAsice createCmsEncryptedAsice;
    private final CreateSendMessageInput createSendMessageInput;
    private final Corner2Client corner2Client;
    private final MessageUnwrapper messageUnwrapper;
    private final KeystoreHelper keystoreHelper;
    private final CreateManifest createManifest;

    @Override
    @SneakyThrows
    public void sendMessage(Shipment shipment) {
        try (InMemoryWithTempFileFallbackResource cmsEncryptedAsice = createCmsEncryptedAsice(shipment)) {
            SendMessageInput input = createSendMessageInput.createSendMessageInput(shipment, cmsEncryptedAsice);
            corner2Client.sendMessage(input);
        } catch (DpiException e) {
            throw e;
        } catch (Exception e) {
            throw new DpiException("Sending failed!", e, Blame.CLIENT);
        }
    }

    private InMemoryWithTempFileFallbackResource createCmsEncryptedAsice(Shipment shipment) {
        InMemoryWithTempFileFallbackResource resource = resourceFactory.getResource("dpi-", ".asic.cms");

        createCmsEncryptedAsice.createCmsEncryptedAsice(CreateCMSEncryptedAsice.Input.builder()
                        .documents(shipment.getParcel().getDocuments())
                        .manifest(createManifest.createManifest(shipment))
                        .certificate(shipment.getReceiverBusinessCertificate())
                        .signatureMethod(SignatureMethod.XAdES)
                        .signatureHelper(keystoreHelper.getSignatureHelper())
                        .keyEncryptionScheme(CmsAlgorithm.RSAES_OAEP)
                        .build(),
                resource
        );

        return resource;
    }

    @Override
    public Flux<MessageStatus> getMessageStatuses(UUID messageId) {
        return corner2Client.getMessageStatuses(messageId);
    }

    @Override
    public Flux<ReceivedMessage> getMessages(GetMessagesInput input) {
        return corner2Client.getMessages(input)
                .map(messageUnwrapper::unwrap)
                .onErrorContinue(IllegalStateException.class, (e, i) -> log.warn("Unwrapping message failed: {} {}", e, i));
    }

    @Override
    public InMemoryWithTempFileFallbackResource getCmsEncryptedAsice(URI downloadurl) throws DpiException {
        return corner2Client.getCmsEncryptedAsice(downloadurl);
    }

    @Override
    public void markAsRead(UUID messageId) {
        corner2Client.markAsRead(messageId);
    }
}
