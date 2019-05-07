package no.difi.meldingsutveksling.webhooks.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;
import java.time.ZonedDateTime;

@JsonIgnoreProperties({"source", "timestamp"})
public abstract class WebhookEvent extends ApplicationEvent {

    @Getter
    private final ZonedDateTime createdTs;

    WebhookEvent(Clock clock, Object source) {
        super(source);
        this.createdTs = ZonedDateTime.now(clock);
    }

    public abstract String getType();
}
