package no.difi.meldingsutveksling.altinnv3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenInput;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AltinnTokenUtil {

    private final JwtTokenClient jwtTokenClient;
    private final RestClient restClient = RestClient.create();
    private final IntegrasjonspunktProperties properties;

    @Cacheable("altinn.retrieveAltinnAccessToken")
    public String retrieveAltinnAccessToken(List<String> scopes) {
        String maskinportenToken = jwtTokenClient.fetchToken(new JwtTokenInput().setScopes(scopes)).getAccessToken();

        return restClient.get()
            .uri(properties.getDpo().getAltinnTokenExchangeUrl()) //todo properties
            .header("Authorization", "Bearer " + maskinportenToken)
            .retrieve()
            .body(String.class);
    }

}

