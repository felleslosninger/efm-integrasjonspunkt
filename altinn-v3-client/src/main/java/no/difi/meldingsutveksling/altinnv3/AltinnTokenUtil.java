package no.difi.meldingsutveksling.altinnv3;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.move.common.oauth.JwtTokenClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

        var jwt = jwtTokenClient.generateJWT(); // FIXME use scopes with the jwtTokenClient
        var signedJwt = SignedJWT.parse(jwt);
        String jwtString = signedJwt.serialize();

        var tokenResponse = restClient.post().uri("https://test.maskinporten.no/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" + jwtString)
            .retrieve()
            .body(TokenResponse.class);

        log.debug("MaskinportenTokenResponse={}", tokenResponse);
        return tokenResponse.access_token;

    }

    record TokenResponse(String access_token, String token_type, long expires_in, String scope) {}

}

