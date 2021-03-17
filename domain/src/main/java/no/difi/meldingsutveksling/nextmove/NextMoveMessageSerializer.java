package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.difi.meldingsutveksling.MessageType;
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
            gen.writeFieldName(MessageType.ARKIVMELDING.getType());
        } else if (value.getAny() instanceof InnsynskravMessage) {
            gen.writeFieldName(MessageType.INNSYNSKRAV.getType());
        } else if (value.getAny() instanceof AvtaltMessage) {
            gen.writeFieldName(MessageType.AVTALT.getType());
        } else if (value.getAny() instanceof PubliseringMessage) {
            gen.writeFieldName(MessageType.PUBLISERING.getType());
        } else if (value.getAny() instanceof DpiDigitalMessage) {
            gen.writeFieldName(MessageType.DIGITAL.getType());
        } else if (value.getAny() instanceof DigitalDpvMessage) {
            gen.writeFieldName(MessageType.DIGITAL_DPV.getType());
        } else if (value.getAny() instanceof DpiPrintMessage) {
            gen.writeFieldName(MessageType.PRINT.getType());
        } else if (value.getAny() instanceof StatusMessage) {
            gen.writeFieldName(MessageType.STATUS.getType());
        } else if (value.getAny() instanceof ArkivmeldingKvitteringMessage) {
            gen.writeFieldName(MessageType.ARKIVMELDING_KVITTERING.getType());
        } else if (value.getAny() instanceof EinnsynKvitteringMessage) {
            gen.writeFieldName(MessageType.EINNSYN_KVITTERING.getType());
        } else if (value.getAny() instanceof FiksIoMessage) {
            gen.writeFieldName(MessageType.FIKSIO.getType());
        }
        gen.writeObject(value.getAny());
        gen.writeEndObject();
    }
}
