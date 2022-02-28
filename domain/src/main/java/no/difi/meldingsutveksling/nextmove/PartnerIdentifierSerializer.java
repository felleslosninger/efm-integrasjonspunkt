package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;

import java.io.IOException;

public class PartnerIdentifierSerializer extends JsonSerializer<PartnerIdentifier> {

    @Override
    public Class<PartnerIdentifier> handledType() {
        return PartnerIdentifier.class;
    }

    @Override
    public void serialize(PartnerIdentifier value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }
}
