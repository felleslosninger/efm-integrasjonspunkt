package no.difi.meldingsutveksling.dpi.client.domain.messagetypes;

import java.util.Arrays;

public enum Varslingskanal {

    SMS("sms"),
    EPOST("epost");

    private final String value;

    Varslingskanal(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Varslingskanal fromValue(String value) {
        return Arrays.stream(Varslingskanal.values()).filter(p -> p.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown varslingskanal = '%s'".formatted(value)));
    }
}
