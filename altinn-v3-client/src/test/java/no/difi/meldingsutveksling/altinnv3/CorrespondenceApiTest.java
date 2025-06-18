package no.difi.meldingsutveksling.altinnv3;

import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class CorrespondenceApiTest {

    @Test
    public void sendMessage() throws JOSEException, IOException, InterruptedException {
        String insertCorrespondenceString = "{\n" +
                "  \"correspondence\": {\n" +
                "    \"resourceId\": \"eformidling-meldingsteneste-test\",\n" +
                "    \"sender\": \"0192:991825827\",\n" +
                "    \"sendersReference\": \"string\",\n" +
                "    \"content\": {\n" +
                "      \"language\": \"nb\",\n" +
                "      \"messageTitle\": \"Testmelding fra Digdir\",\n" +
                "      \"messageSummary\": \"Testmelding fra Digdir\",\n" +
                "      \"messageBody\": \"Testmelding fra Digdir\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"recipients\": [\n" +
                "    \"urn:altinn:organization:identifier-no:310654302\"\n" + // for personer brukes urn:altinn:person:identifier-no:<fnr>
                "  ]\n" +
                "}";

        // altinn:serviceowner kreves for å bruke tenesta som tenesteeigar, men ein kan bruke tenesta utan å være tenesteeigar
        String accessToken2 = ApiUtils.retrieveAccessToken("altinn:correspondence.write altinn:correspondence.read altinn:serviceowner");
        HttpRequest httpRequest3 = HttpRequest.newBuilder()
                .uri(URI.create("https://platform.tt02.altinn.no/correspondence/api/v1/correspondence"))
                .header("Authorization", "Bearer " + accessToken2)
                //.header("Ocp-Apim-Subscription-Key", "5fb085029c294420ab5c0d1e5a4135e8") // Treng per 13. jan 2025 ikkje i tt02
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        insertCorrespondenceString
                ))
                .build();
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse httpResponse3 = httpClient.send(httpRequest3, HttpResponse.BodyHandlers.ofString());
        System.out.println(httpResponse3.statusCode());
        System.out.println(httpResponse3.body());

        assertEquals(200, httpResponse3.statusCode());

        // TODO fleire kall mot APIet .. laste opp vedlegga som skal med og sende avgårde
    }

}
