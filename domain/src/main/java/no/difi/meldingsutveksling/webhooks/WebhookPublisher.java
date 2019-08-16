package no.difi.meldingsutveksling.webhooks;

import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.MessageStatus;

public interface WebhookPublisher {
    void publish(Conversation conversation, MessageStatus messageStatus);
}
