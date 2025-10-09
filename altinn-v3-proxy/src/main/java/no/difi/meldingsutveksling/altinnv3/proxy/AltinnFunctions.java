package no.difi.meldingsutveksling.altinnv3.proxy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.proxy.properties.AltinnProperties;
import no.difi.meldingsutveksling.altinnv3.proxy.token.TokenProducer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Service
public class AltinnFunctions {

    private final static ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final static List<String> ACCESSLIST_SCOPES = List.of("altinn:resourceregistry/accesslist.read");
    private final static List<String> CORRESPONDENCE_SCOPES = List.of("altinn:correspondence.read", "altinn:correspondence.write", "altinn:serviceowner");

    private final WebClient webClient;
    private final Cache<String, String> tokenCache;

    @Inject
    private AltinnProperties altinn;

    @Inject
    private TokenProducer tokenProducer;

    public void invalidateCache() {
        tokenCache.invalidateAll();
    }

    public AltinnFunctions(WebClient webClient) {
        this.webClient = webClient;
        this.tokenCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(25))
            .maximumSize(1000)
            .build();
    }

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

    public Mono<String> getAccessListToken() {
        var scopes = ACCESSLIST_SCOPES;
        var scopesAsASingleString = scopes.stream().collect(java.util.stream.Collectors.joining(" "));
        var token = tokenCache.getIfPresent(scopesAsASingleString);
        if (token != null) return Mono.just(token);
        return tokenProducer.fetchMaskinportenToken(scopes)
            .flatMap(maskinporten -> tokenProducer.exchangeToAltinnToken(maskinporten))
            .doOnNext(altinn -> tokenCache.put(scopesAsASingleString, altinn));
    }

    public Mono<String> getCorrespondenceToken() {
        var scopes = CORRESPONDENCE_SCOPES;
        var scopesAsASingleString = scopes.stream().collect(java.util.stream.Collectors.joining(" "));
        var token = tokenCache.getIfPresent(scopesAsASingleString);
        if (token != null) return Mono.just(token);
        return tokenProducer.fetchMaskinportenToken(scopes)
            .flatMap(maskinporten -> tokenProducer.exchangeToAltinnToken(maskinporten))
            .doOnNext(altinn -> tokenCache.put(scopesAsASingleString, altinn));
    }

    record AccessListMembers(List<Data> data) {
        record Data(Identifiers identifiers) {
            record Identifiers(
                @JsonProperty("urn:altinn:organization:identifier-no")
                String orgnr) {}
        }
    }

    record TokenPayload(Consumer consumer) {
        record Consumer(String ID) {}
    }

}
