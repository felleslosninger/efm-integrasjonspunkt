package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class NextMoveMessageDeserializer extends StdDeserializer<BusinessMessage> {

    protected NextMoveMessageDeserializer() {
        super(BusinessMessage.class);
    }

    @Override
    public BusinessMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        TreeNode node = p.readValueAsTree();
        if ("arkivmelding".equals(p.currentName())) {
            return p.getCodec().treeToValue(node, ArkivmeldingMessage.class);
        }
        if ("digital".equals(p.currentName())) {
            return p.getCodec().treeToValue(node, DpiDigitalMessage.class);
        }
        if ("print".equals(p.currentName())) {
            return p.getCodec().treeToValue(node, DpiPrintMessage.class);
        }
        if ("innsynskrav".equals(p.currentName())) {
            return p.getCodec().treeToValue(node, DpeInnsynMessage.class);
        }
        if ("publisering".equals(p.currentName())) {
            return p.getCodec().treeToValue(node, DpePubliseringMessage.class);
        }
        return null;
    }
}
