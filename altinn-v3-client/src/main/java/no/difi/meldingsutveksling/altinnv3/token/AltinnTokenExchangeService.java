package no.difi.meldingsutveksling.altinnv3.token;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class AltinnTokenExchangeService implements TokenExchangeService {

    private final RestClient restClient;

    @Override
    public String exchangeToken(String maskinportenToken, String altinnExchangeUrl) {
        return restClient.get()
            .uri(altinnExchangeUrl)
            .header("Authorization", "Bearer " + maskinportenToken)
            .retrieve()
            .body(String.class);
    }

}
