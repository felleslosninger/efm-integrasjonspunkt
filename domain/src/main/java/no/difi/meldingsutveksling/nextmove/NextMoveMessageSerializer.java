package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import java.io.IOException;

public class NextMoveMessageSerializer extends StdSerializer<StandardBusinessDocument> {

    protected NextMoveMessageSerializer() {
        super(StandardBusinessDocument.class);
    }

    @Override
    public void serialize(StandardBusinessDocument value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeFieldName("standardBusinessDocumentHeader");
        gen.writeObject(value.getStandardBusinessDocumentHeader());
        if (value.getAny() instanceof DpoMessage) {
            gen.writeFieldName("dpo");
            gen.writeObject(value.getAny());
        } else if (value.getAny() instanceof DpvMessage) {
            gen.writeFieldName("dpv");
            gen.writeObject(value.getAny());
        } else if (value.getAny() instanceof DpeMessage) {
            gen.writeFieldName("dpe");
            gen.writeObject(value.getAny());
        } else if (value.getAny() instanceof DpiMessage) {
            gen.writeFieldName("dpi");
            gen.writeObject(value.getAny());
        }
        gen.writeEndObject();
    }
}
