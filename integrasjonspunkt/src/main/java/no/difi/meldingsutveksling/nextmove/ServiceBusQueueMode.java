package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;

import java.util.Arrays;

@RequiredArgsConstructor
public enum ServiceBusQueueMode {
    INNSYN("innsyn", ServiceIdentifier.DPE_INNSYN),
    DATA("data", ServiceIdentifier.DPE_DATA);

    private final String fullname;
    private final ServiceIdentifier serviceIdentifier;

    public String fullname() {
        return this.fullname;
    }

    public ServiceIdentifier getServiceIdentifier() {
        return serviceIdentifier;
    }

    public static ServiceBusQueueMode valueOfFullname(String fullname) {
        return Arrays.stream(ServiceBusQueueMode.values())
                .filter(p -> p.fullname().equals(fullname))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("No ServiceBusQueueMode with fullname = '%s'", fullname))
                );
    }
}
