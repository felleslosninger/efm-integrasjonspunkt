package no.difi.meldingsutveksling.altinnv3.systemregister;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.ProblemDetailsParser;
import no.difi.meldingsutveksling.altinnv3.token.TokenProducer;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
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

    @Qualifier("SystemregisterTokenProducer")
    private final TokenProducer tokenProducer;
    private final IntegrasjonspunktProperties props;

    private RestClient restClient = RestClient.builder().defaultStatusHandler(HttpStatusCode::isError, this::getApiException).build();

    private static List<String> SCOPES_FOR_SYSTEMREGISTER = List.of("altinn:authentication/systemregister.write");
    private static List<String> SCOPES_FOR_SYSTEMUSER = List.of("altinn:authentication/systemuser.request.write", "altinn:authentication/systemuser.request.read");
    private static List<String> SCOPES_FOR_DPO = List.of("altinn:broker.read", "altinn:broker.write");
    private static List<String> SCOPES_FOR_DPV = List.of("altinn:correspondence.read", "altinn:correspondence.write");

    private String apiEndpoint;

    @PostConstruct
    public void init() {
        // no trailing slash : https://platform.tt02.altinn.no/authentication/api/v1
        apiEndpoint = props.getDpo().getSystemRegisterUrl();
    }

    // Maps the response format, see "systemregister_vendor_response.json" for example
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SystemEntry(String id, List<AccessPackageEntry> accessPackages, boolean isDeleted, boolean isVisible) {};
    public record AccessPackageEntry(String urn) {};

    public String getAccessToken(List<String> scopes) {
        return tokenProducer.produceToken(scopes);
    }

    public SystemEntry getSystem(String id) {
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);
        return restClient.get()
            .uri(apiEndpoint + "/systemregister/vendor/" + id)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(SystemEntry.class)
            ;
    }

    // Maps the response format, see "systemregister_vendor_bysystem_response.json" for example
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SystemUsersResponse(List<SystemUserEntry> data) {};
    public record SystemUserEntry(String id, String reporteeOrgNo, boolean isDeleted, String externalRef) {};

    public List<SystemUserEntry> getAllSystemUsers(String systemId) {
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);
        var response = restClient.get()
            .uri(apiEndpoint + "/systemuser/vendor/bysystem/" + systemId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(SystemUsersResponse.class)
            ;
        if (response == null) return List.of();
        return response.data();
    }

    public String createSystem(String systemName) {
        String orgNo = props.getOrg().getNumber();
        String clientId = props.getDpo().getOidc().getClientId();
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);
        String body = """
            {
              "id": "%s",
              "vendor": {
                "authority": "iso6523-actorid-upis",
                "ID": "0192:%s"
              },
              "name": {
                "nb": "%s",
                "nn": "%s",
                "en": "%s"
              },
              "description": {
                "nb": "Integrasjonspunkt for %s",
                "nn": "Integrasjonspunkt for %s",
                "en": "Integrasjonspunkt for %s"
              },
              "clientId": [
                "%s"
              ],
              "isVisible": true
            }
            """.formatted(
                systemName, orgNo,
                systemName, systemName, systemName,
                orgNo, orgNo, orgNo,
                clientId
        );

         var res =  restClient.post()
            .uri(apiEndpoint + "/systemregister/vendor/")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
             .header("Content-Type", "application/json")
            .body(body)
            .retrieve()
            .body(String.class);

         return res;
    }

    public String updateAccessPackage(String systemId, String accessPackage) {
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);

        String body = """
            [
                {
                    "urn": "%s"
                }
            ]
            """.formatted(accessPackage);

        var res =  restClient.put()
            .uri(apiEndpoint + "/systemregister/vendor/%s/accesspackages".formatted(systemId))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .body(body)
            .retrieve()
            .toBodilessEntity();

        return res.getStatusCode().toString();
    }

    // Maps the response format, see "systemuser_vendor_request.json" for example
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SystemUsersCreationResponse(String id, String externalRef, String systemId, String partyOrgNo, String status, String confirmUrl) {};

    public SystemUsersCreationResponse createStandardSystemUser(String systemUserName, String systemName, String orgNo, String accessPackage) {
        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMUSER);

        var body = """
            {
              "externalRef": "%s",
              "systemId": "%s",
              "partyOrgNo": "%s",
              "rights": [ ],
              "accesspackages": [ {"urn": "%s"} ],
              "redirectUrl": ""
            }
            """.formatted(systemUserName, systemName, orgNo, accessPackage);

        var res =  restClient.post()
            .uri(apiEndpoint + "/systemuser/request/vendor")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .body(body)
            .retrieve()
            .body(SystemUsersCreationResponse.class)
            ;

        return res;
    }

//    public String getSystemUser(String party, String systemUserUuid) {
//        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);
//        return restClient.get()
//            .uri("https://platform.tt02.altinn.no/authentication/api/v1/systemuser/%s/%s".formatted(party, systemUserUuid))
//            .header("Authorization", "Bearer " + accessToken)
//            .header("Accept", "application/json")
//            .retrieve()
//            .body(String.class)
//            ;
//    }
//
//    public String getAll() {
//        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);
//
//        return restClient.get()
//            .uri(apiEndpoint)
//            .header("Authorization", "Bearer " + accessToken)
//            .header("Accept", "application/json")
//            .retrieve()
//            .body(String.class)
//            ;
//    }
//
//    public String systemDetails(String systemId) {
//        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);
//        return restClient.get()
//            .uri(apiEndpoint + "/vendor/" + systemId)
//            .header("Authorization", "Bearer " + accessToken)
//            .header("Accept", "application/json")
//            .retrieve()
//            .body(String.class)
//            ;
//    }

    private void getApiException(HttpRequest request, ClientHttpResponse response) {
        var prefix = "Api error: %s".formatted(request.getURI());
        var details = ProblemDetailsParser.parseClientHttpResponse(prefix, response);
        throw new RuntimeException(details);
    }

}
