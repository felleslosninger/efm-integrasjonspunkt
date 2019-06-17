package no.difi.meldingsutveksling.webhooks.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WebhookEvent<T extends WebhookContent> extends ApplicationEvent {

    private final T content;

    public WebhookEvent(T content, Object source) {
        super(source);
        this.content = content;
    }
}
