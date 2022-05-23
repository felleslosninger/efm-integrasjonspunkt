package no.difi.meldingsutveksling.dpi.client;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.dpi.client.domain.*;
import no.difi.meldingsutveksling.dpi.client.internal.Corner2Client;
import no.difi.meldingsutveksling.dpi.client.internal.CreateCmsEncryptedAsice;
import no.difi.meldingsutveksling.dpi.client.internal.CreateSendMessageInput;
import no.difi.meldingsutveksling.dpi.client.internal.MessageUnwrapper;
import no.difi.meldingsutveksling.dpi.client.internal.domain.SendMessageInput;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class DpiClientImpl implements DpiClient {

    private final CreateCmsEncryptedAsice createCmsEncryptedAsice;
    private final CreateSendMessageInput createSendMessageInput;
    private final Corner2Client corner2Client;
    private final MessageUnwrapper messageUnwrapper;

    @Override
    @SneakyThrows
    public void sendMessage(Shipment shipment) {
        try (CmsEncryptedAsice cmsEncryptedAsice = createCmsEncryptedAsice.createCmsEncryptedAsice(shipment)) {
            SendMessageInput input = createSendMessageInput.createSendMessageInput(shipment, cmsEncryptedAsice);
            corner2Client.sendMessage(input);
        } catch (DpiException e) {
            throw e;
        } catch (Exception e) {
            throw new DpiException("Sending failed!", e, Blame.CLIENT);
        }
    }

    @Override
    public Flux<MessageStatus> getMessageStatuses(UUID messageId) {
        return corner2Client.getMessageStatuses(messageId);
    }

    @Override
    public Flux<ReceivedMessage> getMessages(GetMessagesInput input) {
        return corner2Client.getMessages(input)
                .map(messageUnwrapper::unwrap)
                .onErrorContinue((e, i) -> log.warn("Unwrapping message failed: {} {}", e, i));
    }

    @Override
    public CmsEncryptedAsice getCmsEncryptedAsice(URI downloadurl) throws DpiException {
        return corner2Client.getCmsEncryptedAsice(downloadurl);
    }

    @Override
    public void markAsRead(UUID messageId) {
        corner2Client.markAsRead(messageId);
    }
}
