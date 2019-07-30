package no.difi.meldingsutveksling.webhooks.filter;

import no.difi.meldingsutveksling.webhooks.event.WebhookContent;

public interface EventFilter {

    String getName();

    boolean supports(WebhookContent content);

    boolean supports(EventFilterOperator operator);

    boolean matches(WebhookContent content, EventFilterOperator operator, String value);
}
