package no.difi.meldingsutveksling.altinnv3.systemregister;

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
public class SystemregisterApiClient {

    @Qualifier("ServiceregisterTokenProducer")
    private final TokenProducer tokenProducer;

    private RestClient restClient = RestClient.builder().defaultStatusHandler(HttpStatusCode::isError, this::getApiException).build();

    private static List<String> SCOPES_FOR_RESOURCE = List.of("altinn:resourceregistry/resource.read", "altinn:resourceregistry/resource.write");
    private static List<String> SCOPES_FOR_ACCESSLISTS = List.of("altinn:resourceregistry/accesslist.read", "altinn:resourceregistry/accesslist.write");

    private String apiEndpoint;

    @PostConstruct
    public void init() {
        apiEndpoint = "https://platform.tt02.altinn.no/authentication/api/v1/systemregister";
    }

    public String getAll() {
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_RESOURCE);

        return restClient.get()
            .uri(apiEndpoint)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(String.class)
            ;
    }

    public String createSystem(){
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_RESOURCE);

        String body = """
            {
              "id": "314240979_meldingsutveksling_dpo",
              "vendor": {
                "ID": "0192:314240979"
              },
              "name": {
                "additionalProp1": "KUL SLITEN TIGER AS meldingsutveksling_dpo",
                "additionalProp2": "KUL SLITEN TIGER AS meldingsutveksling_dpo",
                "additionalProp3": "KUL SLITEN TIGER AS meldingsutveksling_dpo"
              },
              "description": {
                "additionalProp1": "meldingsutveksling_dpo",
                "additionalProp2": "meldingsutveksling_dpo",
                "additionalProp3": "meldingsutveksling_dpo"
              },
              "isDeleted": true,
              "clientId": [
                "826acbbc-ee17-4946-af92-cf4885ebe951"
              ],
              "isVisible": true
            }
            """;

         var res =  restClient.post()
            .uri(apiEndpoint + "/vendor")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .body(body)
            .retrieve()
            .toBodilessEntity();

         return res.getStatusCode().toString();
    }

    public String systemDetails() {
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_RESOURCE);
        return restClient.get()
            .uri(apiEndpoint + "/vendor/991825827_meldingsutveksling_dpo")
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
