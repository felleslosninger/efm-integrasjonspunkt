package no.difi.meldingsutveksling.altinnv3;

import lombok.RequiredArgsConstructor;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenInput;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NewTokenUtil  {
    private final JwtTokenClient jwtTokenClient;
    private final RestClient restClient = RestClient.create();

    @Cacheable("dpoClient.getMaskinportenToken")
    public String retrieveAccessToken(String scopes) {

        String maskinportenToken = jwtTokenClient.fetchToken(new JwtTokenInput()).getAccessToken();

        return restClient.get()
            .uri("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten") //todo properties
            .header("Authorization", "Bearer " + maskinportenToken)
            .retrieve()
            .body(String.class);
    }
}




//        HttpRequest httpRequest2 = HttpRequest.newBuilder()
//            //.uri(URI.create("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten?test=true")) // blir organisasjonen ttd
//            .uri(URI.create("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten")) // blir organisasjonen digdir
//            .header("Authorization", "Bearer " + maskinportenToken)
//            .GET()
//            .build();
//        HttpResponse httpResponse2 = httpClient.send(httpRequest2, HttpResponse.BodyHandlers.ofString());


//        System.out.println(httpResponse2.statusCode());
//
//        String accessToken2 = (String) httpResponse2.body();
