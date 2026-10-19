package no.difi.meldingsutveksling.converter;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class StringToStandardBusinessDocumentConverter implements Converter<String, StandardBusinessDocument> {

    private final ObjectMapper objectMapper;

    @Override
    public StandardBusinessDocument convert(String source) {
        if (source == null) {
            return null;
        }

        return objectMapper.readValue(source, StandardBusinessDocument.class);
    }
}
