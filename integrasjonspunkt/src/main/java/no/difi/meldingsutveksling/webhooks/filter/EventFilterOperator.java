package no.difi.meldingsutveksling.webhooks.filter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.exceptions.WebhookUnknownFilterOperationException;

import java.util.EnumSet;

@Getter
@RequiredArgsConstructor
public enum EventFilterOperator {

    EQUALS("=");

    private final String value;

    public static EventFilterOperator fromString(String s) {
        return EnumSet.allOf(EventFilterOperator.class)
                .stream()
                .filter(p -> p.value.equals(s))
                .findFirst()
                .orElseThrow(() -> new WebhookUnknownFilterOperationException(s));
    }
}