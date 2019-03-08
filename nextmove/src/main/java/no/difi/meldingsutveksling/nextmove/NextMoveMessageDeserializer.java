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
        if ("dpo".equals(p.currentName())) {
            return p.getCodec().treeToValue(node, DpoMessage.class);
        }
        if ("dpv".equals(p.currentName())) {
            return p.getCodec().treeToValue(node, DpvMessage.class);
        }
        if ("dpe".equals(p.currentName())) {
            return p.getCodec().treeToValue(node, DpeMessage.class);
        }
        return null;
    }
}
