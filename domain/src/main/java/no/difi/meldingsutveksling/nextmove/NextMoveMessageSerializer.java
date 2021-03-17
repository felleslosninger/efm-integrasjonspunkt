package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.difi.meldingsutveksling.DocumentType;
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
            gen.writeFieldName(DocumentType.ARKIVMELDING.getType());
        } else if (value.getAny() instanceof InnsynskravMessage) {
            gen.writeFieldName(DocumentType.INNSYNSKRAV.getType());
        } else if (value.getAny() instanceof AvtaltMessage) {
            gen.writeFieldName(DocumentType.AVTALT.getType());
        } else if (value.getAny() instanceof PubliseringMessage) {
            gen.writeFieldName(DocumentType.PUBLISERING.getType());
        } else if (value.getAny() instanceof DpiDigitalMessage) {
            gen.writeFieldName(DocumentType.DIGITAL.getType());
        } else if (value.getAny() instanceof DigitalDpvMessage) {
            gen.writeFieldName(DocumentType.DIGITAL_DPV.getType());
        } else if (value.getAny() instanceof DpiPrintMessage) {
            gen.writeFieldName(DocumentType.PRINT.getType());
        } else if (value.getAny() instanceof StatusMessage) {
            gen.writeFieldName(DocumentType.STATUS.getType());
        } else if (value.getAny() instanceof ArkivmeldingKvitteringMessage) {
            gen.writeFieldName(DocumentType.ARKIVMELDING_KVITTERING.getType());
        } else if (value.getAny() instanceof EinnsynKvitteringMessage) {
            gen.writeFieldName(DocumentType.EINNSYN_KVITTERING.getType());
        } else if (value.getAny() instanceof FiksIoMessage) {
            gen.writeFieldName(DocumentType.FIKSIO.getType());
        }
        gen.writeObject(value.getAny());
        gen.writeEndObject();
    }
}
