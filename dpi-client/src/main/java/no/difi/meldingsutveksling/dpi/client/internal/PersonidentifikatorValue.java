package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
class PersonidentifikatorValue {

    private static final Pattern PATTERN = Pattern.compile("^([A-Z]{2}):(.*)$");

    String countryCode;
    String identifier;

    public static PersonidentifikatorValue of(String value) {
        Matcher matcher = PATTERN.matcher(value);

        if (matcher.find()) {
            return new PersonidentifikatorValue(matcher.group(1), matcher.group(2));
        }

        throw new IllegalArgumentException(String.format("Invalid Personidentifikator: %s. Exepected pattern is %s", value, PATTERN.pattern()));
    }
}
