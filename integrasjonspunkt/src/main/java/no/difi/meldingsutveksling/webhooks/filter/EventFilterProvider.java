package no.difi.meldingsutveksling.webhooks.filter;

import no.difi.meldingsutveksling.exceptions.WebhookUnknownFilterException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EventFilterProvider {

    private final Map<String, EventFilter> name2filter;

    public EventFilterProvider(List<EventFilter> eventFilters) {
        this.name2filter = eventFilters.stream()
                .collect(Collectors.toMap(EventFilter::getName, p -> p));
    }

    public EventFilter getEventFilter(String name) {
        return Optional.ofNullable(name2filter.get(name))
                .orElseThrow(() -> new WebhookUnknownFilterException(name));
    }
}
