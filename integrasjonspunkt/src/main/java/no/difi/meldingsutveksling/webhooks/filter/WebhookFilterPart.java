package no.difi.meldingsutveksling.webhooks.filter;

import lombok.Value;

@Value(staticConstructor = "of")
public class WebhookFilterPart {

    private final EventFilter filter;
    private final EventFilterOperator operator;
    private final String value;
}
