package no.difi.meldingsutveksling.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StringToStandardBusinessDocumentConverter implements Converter<String, StandardBusinessDocument> {

    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public StandardBusinessDocument convert(String source) {
        if (source == null) {
            return null;
        }

        return objectMapper.readValue(source, StandardBusinessDocument.class);
    }
}
