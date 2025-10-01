package no.difi.meldingsutveksling.altinnv3.proxy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;

@Component
public class AltinnFunctions {

    private final WebClient webClient = WebClient.builder().build();

    @Inject
    private Oidc oidc;

    public Mono<Void> sendToAltinnWithDigdirToken(ServerWebExchange exchange, GatewayFilterChain chain, String altinntoken) {
        var request = exchange.getRequest().mutate()
            .header("Authorization", "Bearer " + altinntoken)
            .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    public Mono<List<String>> getAccessList(String token){

        var result = webClient.get()
            .uri("https://platform.tt02.altinn.no/resourceregistry/api/v1" + "/access-lists/{owner}/{accesslist}/members", "digdir", "eformidling-meldingsteneste-test-tilgangsliste")
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .retrieve()
            .bodyToMono(String.class)
            .flatMap(v -> {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    var members = objectMapper.readValue(v, Members.class);

                    var orgnr =  members.data().stream().map(member -> member.identifiers().orgnr).toList();

                    return Mono.just(orgnr);
                } catch (Exception e){
                    throw new RuntimeException(e);
                }
            });

        return result;
    };

    record Members (List<Data> data){
        record Data (
            Identifiers identifiers){
            record Identifiers (
                @JsonProperty("urn:altinn:organization:identifier-no")
                String orgnr){}
        }
    }

    public Mono<Void> checkAccessList(ServerWebExchange exchange, List<String> members) {
        try {
            var fewio = exchange.getRequest().getHeaders().getFirst("Authorization").substring(7);
            var jsonorgnr = new String(Base64.getDecoder().decode(fewio.split("\\.")[1]));

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            var orgNr = objectMapper.readValue(jsonorgnr, Tokent.class).consumer().ID.substring(5);

            if (!members.contains(orgNr)) throw new AuthorizationDeniedException("Access denied!!!");

            return Mono.empty();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    record Tokent ( Consumer consumer ){
        record Consumer (String ID){}
    }

    public Mono<String> exchangeToken(String maskinportenToken) {
        var result =  webClient.get()
                .uri("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten")
                .header("Authorization", "Bearer " + maskinportenToken)
                .retrieve()
                .bodyToMono(String.class);

        return result;
    }

    public Mono<String> getCorrespondenceToken(){
        return getMaskinportenToken(List.of("altinn:correspondence.read", "altinn:correspondence.write", "altinn:serviceowner"));
    }

    public Mono<String> getAccessListToken(){
        return getMaskinportenToken(List.of("altinn:resourceregistry/accesslist.read"));
    }

    private Mono<String> getMaskinportenToken(List<String> scopes){

        var jwtTokenConfig = new JwtTokenConfig(
            oidc.clientId(),
            oidc.url(),
            oidc.audience(),
            scopes,
            oidc.keystore()
        );

        var jtc = new JwtTokenClient(jwtTokenConfig);

        return jtc.fetchTokenMono().flatMap(tr -> Mono.just(tr.getAccessToken()));
    }

}
