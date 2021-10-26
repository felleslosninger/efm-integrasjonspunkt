package no.difi.meldingsutveksling.dpi.client;

import no.difi.meldingsutveksling.dpi.client.domain.*;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.UUID;

public interface DpiClient {
    void sendMessage(Shipment shipment) throws DpiException;

    Flux<MessageStatus> getMessageStatuses(UUID messageId) throws DpiException;

    Flux<ReceivedMessage> getMessages(GetMessagesInput input) throws DpiException;

    CmsEncryptedAsice getCmsEncryptedAsice(URI downloadurl) throws DpiException;

    void markAsRead(UUID messageId) throws DpiException;
}
