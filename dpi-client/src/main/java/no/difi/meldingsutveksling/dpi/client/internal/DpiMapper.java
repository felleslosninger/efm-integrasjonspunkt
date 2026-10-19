package no.difi.meldingsutveksling.dpi.client.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.client.domain.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DpiMessageType;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Varslingskanal;
import no.difi.meldingsutveksling.jackson.StandardBusinessDocumentModule;
import org.springframework.core.io.Resource;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import java.io.InputStream;
import java.util.Map;

public class DpiMapper {

    @Getter(AccessLevel.PACKAGE)
    private final ObjectMapper objectMapper;

    public DpiMapper() {
        // Jackson 3: java.time-støtte er innebygd, JavaTimeModule trengst ikkje lenger
        SimpleModule varslingskanalModule = new SimpleModule()
                .addSerializer(new VarslingskanalSerializer())
                .addDeserializer(Varslingskanal.class, new VarslingskanalDeserializer());
        this.objectMapper = JsonMapper.builder()
                .addModule(varslingskanalModule)
                .addModule(new StandardBusinessDocumentModule(DpiMessageType::fromType))
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }

    @SneakyThrows
    public StandardBusinessDocument readStandardBusinessDocument(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, StandardBusinessDocumentWrapper.class).getStandardBusinessDocument();
        }
    }

    public StandardBusinessDocument readStandardBusinessDocument(String s) {
        return objectMapper.readValue(s, StandardBusinessDocumentWrapper.class).getStandardBusinessDocument();
    }

    public Map<String, Object> convertToJsonObject(StandardBusinessDocument standardBusinessDocument) {
        return objectMapper.convertValue(new StandardBusinessDocumentWrapper(standardBusinessDocument), new TypeReference<Map<String, Object>>() {
        });
    }

    private static class VarslingskanalSerializer extends StdSerializer<Varslingskanal> {

        VarslingskanalSerializer() {
            super(Varslingskanal.class);
        }

        @Override
        public void serialize(Varslingskanal value, JsonGenerator gen, SerializationContext context) {
            gen.writeString(value.getValue());
        }
    }

    private static class VarslingskanalDeserializer extends StdDeserializer<Varslingskanal> {

        VarslingskanalDeserializer() {
            super(Varslingskanal.class);
        }

        @Override
        public Varslingskanal deserialize(JsonParser p, DeserializationContext ctxt) {
            return Varslingskanal.fromValue(p.getString());
        }
    }
}
