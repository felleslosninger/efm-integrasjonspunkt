package no.difi.meldingsutveksling.dpi.client.internal;

import no.difi.meldingsutveksling.dpi.client.DpiException;
import no.difi.meldingsutveksling.dpi.client.domain.CmsEncryptedAsice;
import no.difi.meldingsutveksling.dpi.client.domain.GetMessagesInput;
import no.difi.meldingsutveksling.dpi.client.domain.Message;
import no.difi.meldingsutveksling.dpi.client.domain.MessageStatus;
import no.difi.meldingsutveksling.dpi.client.internal.domain.SendMessageInput;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.UUID;

public interface Corner2Client {
    void sendMessage(SendMessageInput input);

    Flux<MessageStatus> getMessageStatuses(UUID messageId);

    Flux<Message> getMessages(GetMessagesInput input);

    CmsEncryptedAsice getCmsEncryptedAsice(URI downloadurl) throws DpiException;

    void markAsRead(UUID messageId);
}
