package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.ValidationException;
import net.jimblackler.jsonschemafriend.Validator;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class JsonDigitalPostSchemaValidator {

    private final Validator validator;
    private final Map<String, Schema> schemaMap;

    public void validate(Object document, String type) {
        validate(document, getSchema(type));
    }

    private Schema getSchema(String type) {
        return Optional.ofNullable(schemaMap.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Unknown standardBusinessDocument.standardBusinessDocumentHeader.documentIdentification.type = %s. Expecting one of %s".formatted(type, String.join(",", schemaMap.keySet()))));
    }

    private void validate(Object document, Schema schema) {
        try {
            validator.validate(schema, document);
        } catch (ValidationException e) {
            throw new IllegalStateException("Validation of Digital Post SBD failed!", e);
        }
    }
}
