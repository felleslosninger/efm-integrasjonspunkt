package no.difi.meldingsutveksling;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UUIDSenderReferenceGenerator implements SenderReferenceGenerator {
    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
