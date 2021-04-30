package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.BiMap;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import java.io.IOException;

public class NextMoveMessageSerializer extends StdSerializer<StandardBusinessDocument> {

    private final BiMap<Class<?>, String> bms;

    protected NextMoveMessageSerializer() {
        super(StandardBusinessDocument.class);
        bms = BusinessMessageUtil.getBusinessMessages().inverse();
    }

    @Override
    public void serialize(StandardBusinessDocument value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeFieldName("standardBusinessDocumentHeader");
        gen.writeObject(value.getStandardBusinessDocumentHeader());

        if (bms.containsKey(value.getAny().getClass())) {
            gen.writeFieldName(bms.get(value.getAny().getClass()));
        } else {
            throw new NextMoveRuntimeException("Unknown business message type: " + value.getAny().getClass());
        }

        gen.writeObject(value.getAny());
        gen.writeEndObject();
    }
}
