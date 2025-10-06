package no.difi.meldingsutveksling.altinnv3.proxy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.altinnv3.proxy.properties.AltinnProperties;
import no.difi.meldingsutveksling.altinnv3.proxy.properties.Oidc;
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
@RequiredArgsConstructor
public class AltinnFunctions {

//    private final WebClient webClient = WebClient.builder().build();
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final List<String> ACCESSLIST_SCOPE = List.of("altinn:resourceregistry/accesslist.read");
    private final List<String> CORRESPONDENCE_SCOPES = List.of("altinn:correspondence.read", "altinn:correspondence.write", "altinn:serviceowner");

    @Inject
    private Oidc oidc;

    @Inject
    private AltinnProperties altinn;

    public Mono<ServerWebExchange> setDigdirTokenInHeaders(ServerWebExchange exchange, GatewayFilterChain chain, String altinntoken) {
        var request = exchange.getRequest().mutate()
            .header("Authorization", "Bearer " + altinntoken)
            .build();

        var newExchange = exchange.mutate().request(request).build();

        return Mono.just(newExchange);
    }

    public Mono<List<String>> getAccessList(String token){

        return webClient.get()
            .uri(altinn.baseUrl() + "/resourceregistry/api/v1/access-lists/{owner}/{accesslist}/members", altinn.accessListOwner(), altinn.accessList())
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .retrieve()
            .bodyToMono(String.class)
            .flatMap(membersInJsonFormat -> {
                try {
                    var members = objectMapper.readValue(membersInJsonFormat, AccessListMembers.class);
                    var orgnr =  members.data().stream().map(member -> member.identifiers().orgnr).toList();

                    return Mono.just(orgnr);
                } catch (JsonProcessingException e){
                    return Mono.error(new RuntimeException(e));
                }
            });
    };

    public Mono<Void> isOrgOnAccessList(ServerWebExchange exchange, List<String> members) {
        var orgNr = getOrgnrFromToken(exchange);

        if (!members.contains(orgNr)) throw new AuthorizationDeniedException("Access denied. Organization " + orgNr + " is not on access list " + altinn.accessList());

        return Mono.empty();
    }

    private String getOrgnrFromToken(ServerWebExchange exchange) {
        try {
            var authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if(authHeader == null || !authHeader.startsWith("Bearer ")) throw new RuntimeException("Missing authorization header");

            var token = authHeader.substring(7);
            var tokenPayload = new String(Base64.getDecoder().decode(token.split("\\.")[1]));

            return objectMapper.readValue(tokenPayload, TokenPayload.class).consumer().ID.substring(5);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse token", e);
        }
    }

    public Mono<String> exchangeToAltinnToken(String maskinportenToken) {
        return webClient.get()
                .uri(altinn.baseUrl() + "/authentication/api/v1/exchange/maskinporten")
                .header("Authorization", "Bearer " + maskinportenToken)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getCorrespondenceToken(){
        return getMaskinportenToken(CORRESPONDENCE_SCOPES);
    }

    public Mono<String> getAccessListToken(){
        return getMaskinportenToken(ACCESSLIST_SCOPE);
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

    record AccessListMembers(List<Data> data){
        record Data (
            Identifiers identifiers){
            record Identifiers (
                @JsonProperty("urn:altinn:organization:identifier-no")
                String orgnr){}
        }
    }

    record TokenPayload(Consumer consumer ){
        record Consumer (String ID){}
    }
}
