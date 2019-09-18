package no.difi.meldingsutveksling.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Converter
@RequiredArgsConstructor
public class StandardBusinessDocumentConverter implements AttributeConverter<StandardBusinessDocument, String> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(StandardBusinessDocument sbd) {
        if (sbd == null) {
            return null;
        }

        try {
            return getObjectMapper().writeValueAsString(sbd);
        } catch (JsonProcessingException e) {
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
        } catch (IOException e) {
            throw new NextMoveRuntimeException(String.format("Couldn't convert String to SBD: %s", dbData), e);
        }
    }

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

            if (objectMapper == null) {
                throw new IllegalStateException("Couldn't inject ObjectMapper into StandardBusinessDocumentConverter");
            }
        }

        return objectMapper;
    }
}
