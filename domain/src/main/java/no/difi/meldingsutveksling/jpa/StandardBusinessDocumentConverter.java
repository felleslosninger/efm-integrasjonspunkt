package no.difi.meldingsutveksling.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Slf4j
@Converter
@RequiredArgsConstructor
public class StandardBusinessDocumentConverter implements AttributeConverter<StandardBusinessDocument, String> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(StandardBusinessDocument sbdh) {
        try {
            return getObjectMapper().writeValueAsString(sbdh);
        } catch (JsonProcessingException e) {
            log.error("Couldn't convert SBD to String", e);
            return null;
        }
    }

    @Override
    public StandardBusinessDocument convertToEntityAttribute(String dbData) {
        try {
            return getObjectMapper().readValue(dbData, StandardBusinessDocument.class);
        } catch (IOException e) {
            log.error("Couldn't convert String to SBD", e);
            return null;
        }
    }

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        }

        return objectMapper;
    }
}
