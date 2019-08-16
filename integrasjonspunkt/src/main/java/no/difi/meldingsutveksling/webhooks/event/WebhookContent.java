package no.difi.meldingsutveksling.webhooks.event;

import java.io.Serializable;
import java.time.OffsetDateTime;

public interface WebhookContent extends Serializable {

    OffsetDateTime getCreatedTs();

    String getResource();

    String getEvent();
}
