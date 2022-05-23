package no.difi.meldingsutveksling.dpi.client.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.client.domain.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DpiMessageType;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Varslingskanal;
import no.difi.meldingsutveksling.jackson.StandardBusinessDocumentModule;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class DpiMapper {

    @Getter(AccessLevel.PACKAGE)
    private final ObjectMapper objectMapper;

    public DpiMapper() {
        this.objectMapper = Jackson2ObjectMapperBuilder.json()
                .serializers(new VarslingskanalSerializer())
                .deserializers(new VarslingskanalDeserializer())
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .modulesToInstall(new JavaTimeModule(), new StandardBusinessDocumentModule(DpiMessageType::fromType))
                .build();
    }

    @SneakyThrows
    public StandardBusinessDocument readStandardBusinessDocument(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, StandardBusinessDocumentWrapper.class).getStandardBusinessDocument();
        }
    }

    @SneakyThrows
    public StandardBusinessDocument readStandardBusinessDocument(String s) {
        return objectMapper.readValue(s, StandardBusinessDocumentWrapper.class).getStandardBusinessDocument();
    }

    @SneakyThrows
    public Map<String, Object> convertToJsonObject(StandardBusinessDocument standardBusinessDocument) {
        return objectMapper.convertValue(new StandardBusinessDocumentWrapper(standardBusinessDocument), new TypeReference<Map<String, Object>>() {
        });
    }

    private static class VarslingskanalSerializer extends StdSerializer<Varslingskanal> {

        VarslingskanalSerializer() {
            super(Varslingskanal.class);
        }

        @Override
        public void serialize(Varslingskanal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.getValue());
        }
    }

    private static class VarslingskanalDeserializer extends StdDeserializer<Varslingskanal> {

        VarslingskanalDeserializer() {
            super(Varslingskanal.class);
        }

        @Override
        public Varslingskanal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return Varslingskanal.fromValue(p.getText());
        }
    }
}
