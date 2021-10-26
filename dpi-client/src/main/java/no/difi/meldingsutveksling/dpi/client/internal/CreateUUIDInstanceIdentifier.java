package no.difi.meldingsutveksling.dpi.client.internal;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateUUIDInstanceIdentifier implements CreateInstanceIdentifier {

    @Override
    public String createInstanceIdentifier() {
        return UUID.randomUUID().toString();
    }
}
