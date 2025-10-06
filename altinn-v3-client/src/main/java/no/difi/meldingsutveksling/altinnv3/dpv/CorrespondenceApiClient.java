package no.difi.meldingsutveksling.altinnv3.dpv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.ProblemDetailsParser;
import no.difi.meldingsutveksling.altinnv3.token.TokenProducer;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.correspondence.model.AttachmentDetailsExt;
import no.digdir.altinn3.correspondence.model.CorrespondenceDetailsExt;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondencesExt;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondencesResponseExt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
@RequiredArgsConstructor
public class CorrespondenceApiClient {

    @Qualifier("DpvTokenProducer")
    private final TokenProducer tokenProducer;
    private final DotNotationFlattener jsonFlatter;
    private final IntegrasjonspunktProperties props;

    private ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule()
            .addDeserializer(OffsetDateTime.class, new AltinnOffsetDateTimeDeserializer()))
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private RestClient restClient = RestClient.builder()
        .defaultStatusHandler(HttpStatusCode::isError, this::getCorrespondenceApiException)
        .messageConverters(this::addJacksonAsConverter)
        .build();

    private static List<String> scopes = List.of("altinn:broker.read");

    private String correspondenceServiceUrl;

    @PostConstruct
    public void init() {
        correspondenceServiceUrl = props.getDpv().getCorrespondenceServiceUrl();
    }

    public void connectionTest() {
        restClient.get()
            .uri(props.getDpv().getHealthCheckUrl())
            .header("Accept", "application/json")
            .retrieve()
            .toBodilessEntity()
        ;
    }

    public AttachmentDetailsExt getAttachmentDetails(UUID attachmentId) {
        String accessToken = tokenProducer.produceToken(scopes);

        return restClient.get()
            .uri(correspondenceServiceUrl + "/attachment/{attachmentId}/details", attachmentId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(AttachmentDetailsExt.class)
            ;
    }

    public CorrespondenceDetailsExt getCorrespondenceDetails(UUID correspondenceId) {
        String accessToken = tokenProducer.produceToken(scopes);

        return restClient.get()
            .uri(correspondenceServiceUrl + "/correspondence/{correspondenceId}/details", correspondenceId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(CorrespondenceDetailsExt.class)
            ;
    }

    public byte[] downloadAttachment(UUID correspondenceId, UUID attachmentId) {
        String accessToken = tokenProducer.produceToken(scopes);

        return restClient.get()
            .uri(correspondenceServiceUrl + "/correspondence/{correspondenceId}/attachment/{attachmentId}/download", correspondenceId, attachmentId)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(byte[].class)
            ;
    }

    public InitializeCorrespondencesResponseExt upload(InitializeCorrespondencesExt request, List<FileUploadRequest> files) {
        String accessToken = tokenProducer.produceToken(scopes);

        Map<String, String> requestValues = jsonFlatter.flatten(request);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        requestValues.forEach(builder::part);
        files.forEach(file -> builder
            .part("attachments", file.getFile())
            .filename(file.getBusinessMessageFile().getFilename()));

        var body = builder.build();

        return restClient.post()
            .uri(correspondenceServiceUrl + "/correspondence/upload")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(InitializeCorrespondencesResponseExt.class)
            ;
    }

    private void getCorrespondenceApiException(HttpRequest request, ClientHttpResponse response) {
        var prefix = "Correspondence api error: %s [%s]".formatted(request.getURI(), request.getURI().getPath());
        var details = ProblemDetailsParser.parseClientHttpResponse(prefix, response);
        throw new CorrespondenceApiException(details);
    }

    private void addJacksonAsConverter(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                jacksonConverter.setObjectMapper(objectMapper);
            }
        }
    }

}
