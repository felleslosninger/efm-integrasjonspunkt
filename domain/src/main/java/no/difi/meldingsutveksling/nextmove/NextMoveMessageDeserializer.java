package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import no.difi.meldingsutveksling.MessageType;

import java.io.IOException;

public class NextMoveMessageDeserializer extends StdDeserializer<BusinessMessage> {

    protected NextMoveMessageDeserializer() {
        super(BusinessMessage.class);
    }

    @Override
    public BusinessMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        TreeNode node = p.readValueAsTree();
        if (MessageType.ARKIVMELDING.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, ArkivmeldingMessage.class);
        }
        if (MessageType.AVTALT.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, AvtaltMessage.class);
        }
        if (MessageType.DIGITAL.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, DpiDigitalMessage.class);
        }
        if (MessageType.DIGITAL_DPV.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, DigitalDpvMessage.class);
        }
        if (MessageType.PRINT.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, DpiPrintMessage.class);
        }
        if (MessageType.INNSYNSKRAV.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, InnsynskravMessage.class);
        }
        if (MessageType.PUBLISERING.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, PubliseringMessage.class);
        }
        if (MessageType.STATUS.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, StatusMessage.class);
        }
        if (MessageType.ARKIVMELDING_KVITTERING.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, ArkivmeldingKvitteringMessage.class);
        }
        if (MessageType.EINNSYN_KVITTERING.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, ArkivmeldingKvitteringMessage.class);
        }
        if (MessageType.FIKSIO.getType().equals(p.getCurrentName())) {
            return p.getCodec().treeToValue(node, FiksIoMessage.class);
        }
        return null;
    }
}
