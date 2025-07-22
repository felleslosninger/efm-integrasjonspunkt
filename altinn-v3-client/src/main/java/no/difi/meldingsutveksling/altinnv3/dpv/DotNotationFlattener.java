package no.difi.meldingsutveksling.altinnv3.dpv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondencesExt;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DotNotationFlattener {
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public Map<String, String> flatten(InitializeCorrespondencesExt request) {
        JsonNode json = objectMapper.valueToTree(request);

        return flattenJson(json);
    }

    private Map<String, String> flattenJson(JsonNode json){
        Map<String, String> values = new LinkedHashMap<>();

        flattenRecursively("", json, values);

        return values;
    }

    private static void flattenRecursively(String prefix, JsonNode node, Map<String, String> values) {
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(field -> {
                String newPrefix = prefix.isEmpty() ? field : prefix + "." + field;
                flattenRecursively(newPrefix, node.get(field), values);
            });
        } else if (node.isArray()) {
            int index = 0;
            for (JsonNode item : node) {
                String newPrefix = prefix + "[" + index + "]";
                flattenRecursively(newPrefix, item, values);
                index++;
            }
        }
        else {
            if (!node.isNull()) values.put(prefix, node.asText()); // todo check and test if this is correct behaviour
        }
    }
}
