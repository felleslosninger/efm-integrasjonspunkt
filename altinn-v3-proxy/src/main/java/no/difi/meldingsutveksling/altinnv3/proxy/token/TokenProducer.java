package no.difi.meldingsutveksling.altinnv3.proxy.token;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.proxy.properties.AltinnProperties;
import no.difi.meldingsutveksling.altinnv3.proxy.properties.Oidc;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class TokenProducer {

    @Inject
    Oidc oidc;

    @Inject
    AltinnProperties altinn;

    @Inject
    WebClient webClient;

    @Inject
    MeterRegistry meterRegistry;

    public Mono<String> fetchMaskinportenToken(List<String> scopes) {
        var jwtTokenConfig = new JwtTokenConfig(
            oidc.clientId(),
            oidc.url(),
            oidc.audience(),
            scopes,
            oidc.keystore()
        );
        var jtc = new JwtTokenClient(jwtTokenConfig);
        var token = jtc.fetchTokenMono().flatMap(tr -> Mono.just(tr.getAccessToken()));
        meterRegistry.counter("eformidling.dpv.proxy.token", "type", "maskinporten").increment();
        return token;
    }

    public Mono<String> exchangeToAltinnToken(String maskinportenToken) {
        var token = webClient.get()
            .uri(altinn.baseUrl() + "/authentication/api/v1/exchange/maskinporten")
            .header("Authorization", "Bearer " + maskinportenToken)
            .retrieve()
            .bodyToMono(String.class);
        meterRegistry.counter("eformidling.dpv.proxy.token", "type", "altinn").increment();
        return token;
    }

}
