package no.difi.meldingsutveksling.altinnv3;

import org.springframework.web.client.RestClient;

public class AltinnTokenSwapper
{
    private final RestClient restClient;

    public AltinnTokenSwapper(RestClient restClient){
        this.restClient = restClient;
    }

    public String getAltinnToken(String maskinportenToken, String altinnTokenExchangeUrl){
        return restClient.get()
            .uri(altinnTokenExchangeUrl)
            .header("Authorization", "Bearer " + maskinportenToken)
            .retrieve()
            .body(String.class);
    }
}
