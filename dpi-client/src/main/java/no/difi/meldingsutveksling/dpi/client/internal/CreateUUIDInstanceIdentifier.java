package no.difi.meldingsutveksling.dpi.client.internal;

import java.util.UUID;

public class CreateUUIDInstanceIdentifier implements CreateInstanceIdentifier {

    @Override
    public String createInstanceIdentifier() {
        return UUID.randomUUID().toString();
    }
}
