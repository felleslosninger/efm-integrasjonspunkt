package no.difi.meldingsutveksling.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperHolder {

    private static ObjectMapper objectMapper;

    public ObjectMapperHolder(ObjectMapper objectMapper) {
        ObjectMapperHolder.objectMapper = objectMapper;
    }

    public static ObjectMapper get() {
        return objectMapper;
    }
}
