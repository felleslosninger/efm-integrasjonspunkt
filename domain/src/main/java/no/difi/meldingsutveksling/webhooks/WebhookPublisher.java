package no.difi.meldingsutveksling.webhooks;

import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;

public interface WebhookPublisher {
    void publish(Conversation conversation, MessageStatus messageStatus);
}
