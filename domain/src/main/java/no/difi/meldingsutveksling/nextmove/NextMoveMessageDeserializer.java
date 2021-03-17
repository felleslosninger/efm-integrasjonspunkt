package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import no.difi.meldingsutveksling.DocumentType;

import java.io.IOException;

public class NextMoveMessageDeserializer extends StdDeserializer<BusinessMessage> {

    protected NextMoveMessageDeserializer() {
        super(BusinessMessage.class);
    }

    @Override
    public BusinessMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        TreeNode node = p.readValueAsTree();
        if (DocumentType.ARKIVMELDING.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, ArkivmeldingMessage.class);
        }
        if (DocumentType.AVTALT.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, AvtaltMessage.class);
        }
        if (DocumentType.DIGITAL.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, DpiDigitalMessage.class);
        }
        if (DocumentType.DIGITAL_DPV.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, DigitalDpvMessage.class);
        }
        if (DocumentType.PRINT.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, DpiPrintMessage.class);
        }
        if (DocumentType.INNSYNSKRAV.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, InnsynskravMessage.class);
        }
        if (DocumentType.PUBLISERING.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, PubliseringMessage.class);
        }
        if (DocumentType.STATUS.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, StatusMessage.class);
        }
        if (DocumentType.ARKIVMELDING_KVITTERING.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, ArkivmeldingKvitteringMessage.class);
        }
        if (DocumentType.EINNSYN_KVITTERING.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, ArkivmeldingKvitteringMessage.class);
        }
        if (DocumentType.FIKSIO.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, FiksIoMessage.class);
        }
        return null;
    }
}
