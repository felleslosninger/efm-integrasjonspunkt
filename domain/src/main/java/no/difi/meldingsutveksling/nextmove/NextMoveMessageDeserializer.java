package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.collect.HashBiMap;

import java.io.IOException;

public class NextMoveMessageDeserializer extends StdDeserializer<BusinessMessage<?>> {

    private final HashBiMap<String, Class<?>> bms;

    protected NextMoveMessageDeserializer() {
        super(BusinessMessage.class);
        bms = BusinessMessageUtil.getBusinessMessages();
    }

    @Override
    public BusinessMessage<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        TreeNode node = p.readValueAsTree();

        if (bms.containsKey(p.getCurrentName())) {
            Class<?> valueType = bms.get(p.getCurrentName());
            return (BusinessMessage<?>) p.getCodec().treeToValue(node, valueType);
        }

        return null;
    }
}
