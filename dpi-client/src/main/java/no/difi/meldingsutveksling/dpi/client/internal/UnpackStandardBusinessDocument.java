package no.difi.meldingsutveksling.dpi.client.internal;

import com.nimbusds.jose.Payload;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

@RequiredArgsConstructor
public class UnpackStandardBusinessDocument {

    private final JsonDigitalPostSchemaValidator jsonDigitalPostSchemaValidator;
    private final DpiMapper dpiMapper;

    public StandardBusinessDocument unpackStandardBusinessDocument(Payload payload) {
        StandardBusinessDocument standardBusinessDocument = dpiMapper.readStandardBusinessDocument(payload.toString());
        String type = standardBusinessDocument.getType().orElse(null);
        jsonDigitalPostSchemaValidator.validate(payload.toJSONObject(), type);
        return standardBusinessDocument;
    }
}
