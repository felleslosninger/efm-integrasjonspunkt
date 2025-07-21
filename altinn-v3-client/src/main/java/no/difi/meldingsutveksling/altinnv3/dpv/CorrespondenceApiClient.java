package no.difi.meldingsutveksling.altinnv3.dpv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.AltinnTokenUtil;
import no.difi.meldingsutveksling.altinnv3.ProblemDetailsParser;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.digdir.altinn3.correspondence.model.*;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
@RequiredArgsConstructor
public class CorrespondenceApiClient {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final RestClient restClient = RestClient.builder()
        .defaultStatusHandler(HttpStatusCode::isError, this::getCorrespondenceApiException)
        .messageConverters(converters -> {
            for (HttpMessageConverter<?> converter : converters) {
                if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                    jacksonConverter.setObjectMapper(objectMapper);
                }
            }
        })
        .build();

    private final String readScope = "altinn:correspondence.read";
    private final String writeScope = "altinn:correspondence.write";
    private final String serviceOwnerScope = "altinn:serviceowner";
    private final AltinnTokenUtil tokenUtil;
    private final DotNotationFlattener jsonFlatter;

    private final IntegrasjonspunktProperties props;

    public UUID initializeAttachment(InitializeAttachmentExt request){
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(writeScope, serviceOwnerScope));

        return restClient.post()
            .uri(props.getDpv().getCorrespondenceServiceUrl() + "/attachment")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .body(request)
            .retrieve()
            .body(UUID.class)
            ;
    }

    public AttachmentOverviewExt uploadAttachment(UUID attachmentId, byte[] bytes) {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(writeScope, serviceOwnerScope));

        var ost =  restClient.post()
            .uri(props.getDpv().getCorrespondenceServiceUrl() + "/attachment/{attachmentId}/upload", attachmentId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/octet-stream")
            .body(bytes)
            .retrieve()
            .body(AttachmentOverviewExt.class)
            ;
        return ost;
    }

    public InitializeCorrespondencesResponseExt initializeCorrespondence(InitializeCorrespondencesExt request){
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(writeScope, serviceOwnerScope));

        return restClient.post()
            .uri(props.getDpv().getCorrespondenceServiceUrl() + "/correspondence")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .body(request)
            .retrieve()
            .body(InitializeCorrespondencesResponseExt.class)
            ;
    }

    public AttachmentDetailsExt getAttachmentDetails(UUID attachmentId) {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, writeScope, serviceOwnerScope));

        return restClient.get()
            .uri(props.getDpv().getCorrespondenceServiceUrl() + "/attachment/{attachmentId}/details", attachmentId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(AttachmentDetailsExt.class)
            ;
    }

    public CorrespondenceDetailsExt getCorrespondenceDetails(UUID correspondenceId) {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, writeScope, serviceOwnerScope));

        return restClient.get()
            .uri(props.getDpv().getCorrespondenceServiceUrl() + "/correspondence/{correspondenceId}/details", correspondenceId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(CorrespondenceDetailsExt.class)
            ;
    }

    public byte[] downloadAttachment(UUID correspondenceId, UUID attachmentId) {
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, writeScope, serviceOwnerScope));

        return restClient.get()
            .uri(props.getDpv().getCorrespondenceServiceUrl() + "/correspondence/{correspondenceId}/attachment/{attachmentId}/download", correspondenceId, attachmentId)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(byte[].class)
            ;
    }

    public InitializeCorrespondencesResponseExt upload(InitializeCorrespondencesExt request, List<FileUploadRequest> files){
        String accessToken = tokenUtil.retrieveAltinnAccessToken(List.of(readScope, writeScope, serviceOwnerScope));

        Map<String, Object> requestValues = jsonFlatter.flatten(request);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        requestValues.forEach(builder::part);
        files.forEach(file -> builder
            .part("attachments", file.getFile())
            .filename(file.getBusinessMessageFile().getFilename()));

        var body = builder.build();

        return restClient.post()
            .uri(props.getDpv().getCorrespondenceServiceUrl() + "/correspondence/upload")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(InitializeCorrespondencesResponseExt.class)
            ;
    }

    private void getCorrespondenceApiException(HttpRequest request, ClientHttpResponse response) {
        var prefix = "Correspondence api error: %s %s".formatted(request.getURI(), request.getURI().getPath());
        var details = ProblemDetailsParser.parseClientHttpResponse(prefix, response);
        log.error(details); //todo change?
        throw new CorrespondenceApiException(details);
    }

}
