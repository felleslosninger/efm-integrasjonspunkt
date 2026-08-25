package no.difi.meldingsutveksling.jpa;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
@RequiredArgsConstructor
public class StandardBusinessDocumentConverter implements AttributeConverter<StandardBusinessDocument, String> {

    @Override
    public String convertToDatabaseColumn(StandardBusinessDocument sbd) {
        if (sbd == null) {
            return null;
        }

        try {
            return getObjectMapper().writeValueAsString(sbd);
        } catch (JacksonException e) {
            throw new NextMoveRuntimeException("Couldn't convert SBD to String", e);
        }
    }

    @Override
    public StandardBusinessDocument convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        try {
            return getObjectMapper().readValue(dbData, StandardBusinessDocument.class);
        } catch (JacksonException e) {
            throw new NextMoveRuntimeException("Couldn't convert String to SBD: %s".formatted(dbData), e);
        }
    }

    private ObjectMapper getObjectMapper() {
        return ObjectMapperHolder.get();
    }
}
