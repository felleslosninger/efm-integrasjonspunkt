package no.difi.meldingsutveksling.webhooks.event;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class WebhookContentBase<T extends WebhookContent> implements WebhookContent {

    private OffsetDateTime createdTs;
    private String resource;
    private String event;

    public T setCreatedTs(OffsetDateTime createdTs) {
        this.createdTs = createdTs;
        return (T) this;
    }

    public T setResource(String resource) {
        this.resource = resource;
        return (T) this;
    }

    public T setEvent(String event) {
        this.event = event;
        return (T) this;
    }
}
