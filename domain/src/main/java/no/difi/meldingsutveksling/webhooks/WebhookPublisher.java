package no.difi.meldingsutveksling.webhooks;

import no.difi.meldingsutveksling.receipt.MessageStatus;

public interface WebhookPublisher {
    void publish(MessageStatus messageStatus);
}
