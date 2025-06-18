package no.difi.meldingsutveksling.altinnv3;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.move.common.oauth.JwtTokenClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class AltinnTokenUtil {

    private final JwtTokenClient jwtTokenClient;
    private final RestClient restClient = RestClient.create();

    @Cacheable("altinn.retrieveAltinnAccessToken")
    public String retrieveAltinnAccessToken(String scopes) {
        String maskinportenToken = retrieveMaskinportenAccessToken(scopes);
        return restClient.get()
            .uri("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten") //todo properties
            .header("Authorization", "Bearer " + maskinportenToken)
            .retrieve()
            .body(String.class);
    }

    @SneakyThrows
    String retrieveMaskinportenAccessToken(String scopes) {
        // FIXME use scopes with the jwtTokenClient
        var jwt = jwtTokenClient.generateJWT();
        var signedJwt = SignedJWT.parse(jwt);
        String jwtString = signedJwt.serialize();
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://test.maskinporten.no/token"))
            .header("Content-type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(
                "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" + jwtString
            ))
            .build();
        HttpResponse httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        String body = (String) httpResponse.body();
        String marker = "\"access_token\":\"";
        int beginIndex = body.indexOf(marker) + marker.length();
        int endIndex = body.indexOf("\"", beginIndex);
        String accessToken = body.substring(beginIndex, endIndex);
        log.debug("MaskinportenToken={}", accessToken);
        return accessToken;
    }

}
