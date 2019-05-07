package no.difi.meldingsutveksling.webhooks.event;

import lombok.ToString;

import java.time.Clock;

@ToString
public class PingEvent extends WebhookEvent {

    PingEvent(Clock clock, Object source) {
        super(clock, source);
    }

    @Override
    public String getType() {
        return "ping";
    }
}
