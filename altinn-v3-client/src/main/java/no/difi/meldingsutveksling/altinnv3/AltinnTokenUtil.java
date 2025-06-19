package no.difi.meldingsutveksling.altinnv3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.move.common.oauth.JwtTokenClient;
import org.springframework.cache.annotation.Cacheable;
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
        String maskinportenToken = jwtTokenClient.fetchToken().getAccessToken();

        return restClient.get()
            .uri("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten") //todo properties
            .header("Authorization", "Bearer " + maskinportenToken)
            .retrieve()
            .body(String.class);
    }

}

