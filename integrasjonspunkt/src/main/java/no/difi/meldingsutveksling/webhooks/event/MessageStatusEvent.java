package no.difi.meldingsutveksling.webhooks.event;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class MessageStatusEvent extends WebhookEvent {

    private Integer statId;
    private Integer convId;
    private String conversationId;
    private LocalDateTime lastUpdate;
    private String status;
    private String description;

    MessageStatusEvent(Clock clock, Object source) {
        super(clock, source);
    }

    @Override
    public String getType() {
        return "message.status";
    }
}
