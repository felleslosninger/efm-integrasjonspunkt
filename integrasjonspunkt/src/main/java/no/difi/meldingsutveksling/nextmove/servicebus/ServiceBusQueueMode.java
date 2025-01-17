package no.difi.meldingsutveksling.nextmove.servicebus;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
public enum ServiceBusQueueMode {
    INNSYN("innsyn"),
    DATA("data"),
    MEETING("meeting");

    private final String fullname;

    public String fullname() {
        return this.fullname;
    }

    public static ServiceBusQueueMode valueOfFullname(String fullname) {
        return Arrays.stream(ServiceBusQueueMode.values())
                .filter(p -> p.fullname().equals(fullname))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No ServiceBusQueueMode with fullname = '%s'".formatted(fullname))
                );
    }
}
