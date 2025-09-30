package no.difi.meldingsutveksling.altinnv3.proxy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.file.AccessDeniedException;
import java.util.Base64;
import java.util.List;

public class TokenFilter implements GatewayFilter {

    private final Oidc oidc;
    private final WebClient webClient = WebClient.builder().build();

    public TokenFilter(Oidc oidc) {
        this.oidc = oidc;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        //accesslist


//        var token = Mono.just(getToken());

        return getAccessListToken()
            .flatMap(this::getAccessList)
            .flatMap(members -> checkAccessList(exchange, members))
            .then(getCorrespondenceToken())
            .flatMap(this::exchangeToken)
            .flatMap(altinntoken -> sendToAltinnWithDigdirToken(exchange, chain, altinntoken));
//
//        var result = token.flatMap(d -> exchangeToken(d).flatMap(token2 -> {
//            var request = exchange.getRequest().mutate()
//                    .header("Authorization", "Bearer " + token2)
//                    .build();
//
//            return chain.filter(exchange.mutate().request(request).build());
//
//        }));

//        var token = getToken();

//        exchangeToken(token);
//
//        var request = exchange.getRequest().mutate()
//            .header("Authorization", "Bearer " + token)
//            .build();
//
//        return chain.filter(exchange.mutate().request(request).build());
    }

    private static Mono<Void> sendToAltinnWithDigdirToken(ServerWebExchange exchange, GatewayFilterChain chain, String altinntoken) {
        var request = exchange.getRequest().mutate()
            .header("Authorization", "Bearer " + altinntoken)
            .build();

        return chain.filter(exchange.mutate().request(request).build());
    }


    private Mono<List<String>> getAccessList(String token){

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

    private Mono<Void> checkAccessList(ServerWebExchange exchange, List<String> members) {
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

    private Mono<String> exchangeToken(String maskinportenToken) {

        var result =  webClient.get()
                .uri("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten")
                .header("Authorization", "Bearer " + maskinportenToken)
                .retrieve()
                .bodyToMono(String.class);

        return result;
    }

    private Mono<String> getCorrespondenceToken(){
        var token =  getMaskinportenToken(List.of("altinn:correspondence.read", "altinn:correspondence.write", "altinn:serviceowner"));

        return Mono.just(token);
    }

    private Mono<String> getAccessListToken(){
        var token =  getMaskinportenToken(List.of("altinn:resourceregistry/accesslist.read"));

        return Mono.just(token);
    }

    private String getMaskinportenToken(List<String> scopes){

        var jwtTokenConfig = new JwtTokenConfig(
            oidc.clientId(),
            oidc.url(),
            oidc.audience(),
            scopes,
            oidc.keystore()
        );

        var jtc = new JwtTokenClient(jwtTokenConfig);
//        var jti = new JwtTokenInput().setClientId(oidc.clientId()).setScopes(List.of("altinn:correspondence.read", "altinn:correspondence.write", "altinn:serviceowner"));
        return jtc.fetchToken().getAccessToken();
    }
}
