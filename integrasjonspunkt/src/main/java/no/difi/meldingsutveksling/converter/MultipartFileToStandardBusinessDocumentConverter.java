package no.difi.meldingsutveksling.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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
