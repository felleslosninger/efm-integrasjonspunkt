package no.difi.meldingsutveksling.altinnv3.dpv;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.OffsetDateTime;


public class AltinnOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

    String regex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1,6}$";

    @Override
    public OffsetDateTime deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {

        var value = parser.getText();

        if (value.matches(regex)) value = AddSuffix(value);

        return OffsetDateTime.parse(value);
    }

    private String AddSuffix(String value){
        return value +  "Z";
    }
}
