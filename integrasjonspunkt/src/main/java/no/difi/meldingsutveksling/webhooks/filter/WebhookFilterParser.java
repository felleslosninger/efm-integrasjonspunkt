package no.difi.meldingsutveksling.webhooks.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class WebhookFilterParser {

    private static final String OPERATOR_PATTERN = EnumSet.allOf(EventFilterOperator.class)
            .stream()
            .map(EventFilterOperator::getValue)
            .collect(Collectors.joining("|"));

    private static final Pattern PART_PATTERN = Pattern.compile("^(\\w+)(" + OPERATOR_PATTERN + ")(.+)$");

    private final EventFilterProvider eventFilterProvider;

    public Stream<WebhookFilterPart> parse(String filter) {
        return Arrays.stream(filter.split("&"))
                .map(this::getWebhookFilterPart);
    }

    private WebhookFilterPart getWebhookFilterPart(String part) {
        Matcher matcher = PART_PATTERN.matcher(part);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "The filter part \"%s\", do not match the expected regexp: %s".formatted(
                            part, PART_PATTERN.toString()));
        }

        String name = matcher.group(1);
        EventFilter eventFilter = eventFilterProvider.getEventFilter(name);
        EventFilterOperator operator = EventFilterOperator.fromString(matcher.group(2));
        if (!eventFilter.supports(operator)) {
            throw new IllegalArgumentException("The %s filter do not support the %s operator!".formatted(name, operator));
        }

        String value = matcher.group(3);

        return WebhookFilterPart.of(eventFilter, operator, value);
    }
}
