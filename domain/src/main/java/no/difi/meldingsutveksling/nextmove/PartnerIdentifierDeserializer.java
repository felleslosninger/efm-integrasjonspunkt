package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;

import java.io.IOException;

public class PartnerIdentifierDeserializer extends JsonDeserializer<PartnerIdentifier> {

    @Override
    public Class<?> handledType() {
        return PartnerIdentifier.class;
    }

    @Override
    public PartnerIdentifier deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String s = p.readValueAs(String.class);
        return s != null ? PartnerIdentifier.parse(s) : null;
    }
}
