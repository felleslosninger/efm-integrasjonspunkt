package no.difi.meldingsutveksling.dpi.client;

import no.difi.meldingsutveksling.dpi.client.domain.GetMessagesInput;
import no.difi.meldingsutveksling.dpi.client.domain.MessageStatus;
import no.difi.meldingsutveksling.dpi.client.domain.ReceivedMessage;
import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.UUID;

public interface DpiClient {
    void sendMessage(Shipment shipment) throws DpiException;

    Flux<MessageStatus> getMessageStatuses(UUID messageId) throws DpiException;

    Flux<ReceivedMessage> getMessages(GetMessagesInput input) throws DpiException;

    InMemoryWithTempFileFallbackResource getCmsEncryptedAsice(URI downloadurl) throws DpiException;

    void markAsRead(UUID messageId) throws DpiException;
}
