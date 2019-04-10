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
        if (value.getAny() instanceof ArkivmeldingMessage) {
            gen.writeFieldName("arkivmelding");
            gen.writeObject(value.getAny());
        } else if (value.getAny() instanceof DpeInnsynMessage) {
            gen.writeFieldName("innsynskrav");
            gen.writeObject(value.getAny());
        } else if (value.getAny() instanceof DpePubliseringMessage) {
            gen.writeFieldName("publisering");
            gen.writeObject(value.getAny());
        } else if (value.getAny() instanceof DpiDigitalMessage) {
            gen.writeFieldName("digital");
            gen.writeObject(value.getAny());
        } else if (value.getAny() instanceof DpiPrintMessage) {
            gen.writeFieldName("print");
            gen.writeObject(value.getAny());
        }
        gen.writeEndObject();
    }
}
