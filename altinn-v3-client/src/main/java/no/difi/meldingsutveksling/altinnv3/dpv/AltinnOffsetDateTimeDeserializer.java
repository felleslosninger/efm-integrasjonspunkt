package no.difi.meldingsutveksling.altinnv3.dpv;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.OffsetDateTime;


public class AltinnOffsetDateTimeDeserializer extends ValueDeserializer<OffsetDateTime> {

    String regex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1,6}$";

    @Override
    public OffsetDateTime deserialize(JsonParser parser, DeserializationContext ctxt) {

        var value = parser.getString();

        if (value.matches(regex)) value = AddSuffix(value);

        return OffsetDateTime.parse(value);
    }

    private String AddSuffix(String value){
        return value +  "Z";
    }
}
