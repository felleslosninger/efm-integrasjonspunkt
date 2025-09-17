package no.difi.meldingsutveksling.altinnv3.systemregister;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.ProblemDetailsParser;
import no.difi.meldingsutveksling.altinnv3.token.SystemUserTokenProducer;
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

//    @Qualifier("SystemregisterTokenProducer")
    @Qualifier("SystemuserTokenProducer")
    private final TokenProducer tokenProducer;

    private final SystemUserTokenProducer systemUserTokenProducer = new SystemUserTokenProducer();

    private RestClient restClient = RestClient.builder().defaultStatusHandler(HttpStatusCode::isError, this::getApiException).build();

    private static List<String> SCOPES_FOR_SYSTEMREGISTER = List.of("altinn:authentication/systemregister.write");
    private static List<String> SCOPES_FOR_SYSTEMUSER = List.of("altinn:authentication/systemuser.request.write", "altinn:authentication/systemuser.request.read");

    private String apiEndpoint;

    @PostConstruct
    public void init() {
        apiEndpoint = "https://platform.tt02.altinn.no/authentication/api/v1/systemregister";
    }

    public String getTokenTest(){
        return systemUserTokenProducer.produceToken(List.of("altinn:broker.read", "altinn:broker.write"));
    }

    public String getAllSystemUsers(String systemId){
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);

        return restClient.get()
            .uri("https://platform.tt02.altinn.no/authentication/api/v1/systemuser/vendor/bysystem/" + systemId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(String.class)
            ;
    }

    public String createStandardSystemUser(){
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMUSER);

        var body = """
            {
              "externalRef": "314240979_integrasjonspunkt_systembruker_test2",
              "systemId": "314240979_integrasjonspunkt",
              "partyOrgNo": "314240979",
              "rights": [
                  ],
              "accesspackages": [
                  {
                    "urn": "urn:altinn:accesspackage:maskinlesbare-hendelser"
                  }
                ],
              "redirectUrl": ""
            }
            """;

        var res =  restClient.post()
            .uri("https://platform.tt02.altinn.no/authentication/api/v1/systemuser/request/vendor")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .body(body)
            .retrieve();

        System.out.println(res.toEntity(String.class));
        return "";
    }

    public String createAgentSystemUser(){
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMUSER);

        var body = """
            {
              "externalRef": "314240979_integrasjonspunkt_systembruker_test",
              "systemId": "314240979_integrasjonspunkt",
              "partyOrgNo": "314240979",
              "accesspackages": [
                  {
                    "urn": "urn:altinn:accesspackage:maskinlesbare-hendelser"
                  }
                ],
              "redirectUrl": ""
            }
            """;

        var res =  restClient.post()
//            .uri("https://platform.tt02.altinn.no/authentication/api/v1/systemuser/request/vendor")
            .uri("https://platform.tt02.altinn.no/authentication/api/v1/systemuser/request/vendor/agent")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .body(body)
            .retrieve();

        System.out.println(res.toEntity(String.class));
        return "";
    }

    public String getAll() {
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);

        return restClient.get()
            .uri(apiEndpoint)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(String.class)
            ;
    }

    public String getSystem(String id) {
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);

        return restClient.get()
            .uri(apiEndpoint + "/vendor/" + id)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(String.class)
            ;
    }

    public String updateAccessPackage(){
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);

        String body = """
            [
                {
                    "urn": "urn:altinn:accesspackage:maskinlesbare-hendelser"
                }
            ]
            """;

        //PUT authentication/api/v1/systemregister/vendor/{systemid}/accesspackages


        var res =  restClient.put()
            .uri("https://platform.tt02.altinn.no/authentication/api/v1/systemregister/vendor/" + "314240979_integrasjonspunkt" +"/accesspackages")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .body(body)
            .retrieve()
            .toBodilessEntity();

        return res.getStatusCode().toString();
    }

    public String createSystem(){
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);

        String body = """
            {
              "id": "314240979_integrasjonspunkt",
              "vendor": {
                "authority": "iso6523-actorid-upis",
                "ID": "0192:314240979"
              },
              "name": {
                "nb": "KUL SLITEN TIGER AS integrasjonspunkt",
                "nn": "KUL SLITEN TIGER AS integrasjonspunkt",
                "en": "KUL SLITEN TIGER AS integrasjonspunkt"
              },
              "description": {
                "nb": "integrasjonspunkt",
                "nn": "integrasjonspunkt",
                "en": "integrasjonspunkt"
              },
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
             .header("Content-Type", "application/json")
            .body(body)
            .retrieve()
            .toBodilessEntity();

         return res.getStatusCode().toString();
    }

    public String systemDetails() {
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);
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
