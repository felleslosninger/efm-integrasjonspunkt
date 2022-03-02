package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.SneakyThrows;
import no.difi.meldingsutveksling.dpi.client.internal.domain.SendMessageInput;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;

public class CreateMultipart {

    private static final MediaType APPLICATION_JWT = MediaType.parseMediaType("application/jwt");
    private static final MediaType APPLICATION_CMS = MediaType.parseMediaType("application/cms");

    @SneakyThrows
    public MultiValueMap<String, HttpEntity<?>> createMultipart(SendMessageInput input) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("forretningsmelding", input.getJwt(), APPLICATION_JWT)
                .filename("sbd.jwt");
        builder.part("dokumentpakke", input.getCmsEncryptedAsice().getResource(), APPLICATION_CMS)
                .filename("asic.cms");
        return builder.build();
    }
}
