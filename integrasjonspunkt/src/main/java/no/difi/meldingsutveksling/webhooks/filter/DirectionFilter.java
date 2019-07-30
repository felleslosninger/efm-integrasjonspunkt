package no.difi.meldingsutveksling.webhooks.filter;

import no.difi.meldingsutveksling.webhooks.event.MessageStatusContent;
import no.difi.meldingsutveksling.webhooks.event.WebhookContent;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DirectionFilter implements EventFilter {

    @Override
    public String getName() {
        return "direction";
    }

    @Override
    public boolean supports(WebhookContent content) {
        return content instanceof MessageStatusContent;
    }

    @Override
    public boolean supports(EventFilterOperator operator) {
        return operator == EventFilterOperator.EQUALS;
    }

    @Override
    public boolean matches(WebhookContent content, EventFilterOperator operator, String value) {
        MessageStatusContent messageStatusContent = (MessageStatusContent) content;
        String expected = messageStatusContent.getDirection().name();
        return Arrays.stream(value.split(","))
                .anyMatch(expected::equalsIgnoreCase);
    }
}
