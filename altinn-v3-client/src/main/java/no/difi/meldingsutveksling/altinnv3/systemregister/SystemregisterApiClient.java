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
            .uri(apiEndpoint + "/vendor/" + id)
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
            .uri("https://platform.tt02.altinn.no/authentication/api/v1/systemuser/vendor/bysystem/" + systemId)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(SystemUsersResponse.class)
            ;
        if (response == null) return List.of();
        return response.data();
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
//    public String createStandardSystemUser(String orgNo, String systemId, String name, String accessPackage) {
//        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMUSER);
//
//        var body = """
//            {
//              "externalRef": "%s_systembruker_%s",
//              "systemId": "%s",
//              "partyOrgNo": "%s",
//              "rights": [ ],
//              "accesspackages": [ {"urn": "%s"} ],
//              "redirectUrl": ""
//            }
//            """.formatted(systemId, name, systemId, orgNo, accessPackage);
//
//        var res =  restClient.post()
//            .uri("https://platform.tt02.altinn.no/authentication/api/v1/systemuser/request/vendor")
//            .header("Authorization", "Bearer " + accessToken)
//            .header("Accept", "application/json")
//            .header("Content-Type", "application/json")
//            .body(body)
//            .retrieve()
//            ;
//
//        return res.body(String.class);
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
//    public String updateAccessPackage(String systemId, String accessPackage) {
//        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);
//
//        String body = """
//            [
//                {
//                    "urn": "%s"
//                }
//            ]
//            """.formatted(accessPackage);
//
//        var res =  restClient.put()
//            .uri("https://platform.tt02.altinn.no/authentication/api/v1/systemregister/vendor/%s/accesspackages".formatted(systemId))
//            .header("Authorization", "Bearer " + accessToken)
//            .header("Accept", "application/json")
//            .header("Content-Type", "application/json")
//            .body(body)
//            .retrieve()
//            .toBodilessEntity();
//
//        return res.getStatusCode().toString();
//    }
//
//    public String createSystem(String orgno, String name, String clientId) {
//        String accessToken = tokenProducer.produceToken(SCOPES_FOR_SYSTEMREGISTER);
//
//        String body = """
//            {
//              "id": "%s_integrasjonspunkt",
//              "vendor": {
//                "authority": "iso6523-actorid-upis",
//                "ID": "0192:%s"
//              },
//              "name": {
//                "nb": "%s integrasjonspunkt",
//                "nn": "%s integrasjonspunkt",
//                "en": "%s integrasjonspunkt"
//              },
//              "description": {
//                "nb": "integrasjonspunkt",
//                "nn": "integrasjonspunkt",
//                "en": "integrasjonspunkt"
//              },
//              "clientId": [
//                "%s"
//              ],
//              "isVisible": true
//            }
//            """.formatted(orgno, orgno, name, name, name, clientId);
//
//         var res =  restClient.post()
//            .uri(apiEndpoint + "/vendor")
//            .header("Authorization", "Bearer " + accessToken)
//            .header("Accept", "application/json")
//             .header("Content-Type", "application/json")
//            .body(body)
//            .retrieve()
//            .toBodilessEntity();
//
//         return res.getStatusCode().toString();
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
