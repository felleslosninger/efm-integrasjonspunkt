package no.difi.meldingsutveksling.dph.client.internal;

import lombok.SneakyThrows;
import no.difi.meldingsutveksling.nhn.adapter.model.ContentTypes;
import no.difi.meldingsutveksling.nhn.adapter.model.MultipartFileNames;
import no.difi.meldingsutveksling.nhn.adapter.model.MultipartNames;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;

public class CreateMultipart {

    public static final MediaType APPLICATION_JOSE = MediaType.parseMediaType(ContentTypes.APPLICATION_JOSE);
    public static final MediaType APPLICATION_ASICE = MediaType.parseMediaType(ContentTypes.APPLICATION_ASICE);

    @SneakyThrows
    public MultiValueMap<String, HttpEntity<?>> createMultipart(String forretningsmelding, Resource dokumentpakke) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part(MultipartNames.FORRETNINGSMELDING, forretningsmelding, APPLICATION_JOSE)
            .filename(MultipartFileNames.FORRETNINGSMELDING);
        builder.part(MultipartNames.DOKUMENTPAKKE, dokumentpakke, APPLICATION_ASICE)
            .filename(MultipartFileNames.DOKUMENTPAKKE);
        return builder.build();
    }
}
