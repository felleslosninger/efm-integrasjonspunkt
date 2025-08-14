package no.difi.meldingsutveksling.altinnv3.resource;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.ProblemDetailsParser;
import no.difi.meldingsutveksling.altinnv3.token.TokenProducer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceApiClient {

    @Qualifier("ResourceTokenProducer")
    private final TokenProducer tokenProducer;

    private RestClient restClient = RestClient.builder().defaultStatusHandler(HttpStatusCode::isError, this::getApiException).build();

    private static String readScope = "altinn:resourceregistry/resource.read";
    private static String writeScope = "altinn:resourceregistry/resource.write";
    private static String serviceOwnerScope = "altinn:serviceowner";

    private String apiEndpoint;

    @PostConstruct
    public void init() {
        apiEndpoint = "https://platform.tt02.altinn.no/resourceregistry/api/v1";
    }

    public String resourceOwner() {
        String accessToken = tokenProducer.produceToken(List.of(serviceOwnerScope));
        return restClient.get()
            .uri(apiEndpoint + "/resource/orgs")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(String.class)
            ;
    }

    public String resourceList() {
        String accessToken = tokenProducer.produceToken(List.of(serviceOwnerScope));
        return restClient.get()
            .uri(apiEndpoint + "/resource/resourcelist")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(String.class)
            ;
    }

    public String accessLists() {
        String accessToken = tokenProducer.produceToken(List.of(readScope, writeScope, serviceOwnerScope));
        return restClient.get()
            .uri(apiEndpoint + "/access-lists/{owner}", "digdir") //props.getDpo().getResource())
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(String.class)
            ;
    }

    private void getApiException(HttpRequest request, ClientHttpResponse response) {
        var prefix = "Api error: %s".formatted(request.getURI());
        var details = ProblemDetailsParser.parseClientHttpResponse(prefix, response);
        throw new RuntimeException(details);
    }

}
