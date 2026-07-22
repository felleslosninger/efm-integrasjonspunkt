package no.difi.meldingsutveksling.converter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class MultipartFileToStandardBusinessDocumentConverter implements Converter<MultipartFile, StandardBusinessDocument> {

    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public StandardBusinessDocument convert(MultipartFile source) {
        if (source == null) {
            return null;
        }

        byte[] bytes = source.getBytes();
        return objectMapper.readValue(bytes, StandardBusinessDocument.class);
    }
}
