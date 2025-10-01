package no.difi.meldingsutveksling.altinnv3.proxy;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class TokenFilter implements GatewayFilter {

    private final AltinnFunctions functions;
    private final WebClient webClient = WebClient.builder().build();

    public TokenFilter(AltinnFunctions functions) {
        this.functions = functions;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return functions.getAccessListToken()
            .flatMap(functions::getAccessList)
            .flatMap(members -> functions.checkAccessList(exchange, members))
            .then(functions.getCorrespondenceToken())
            .flatMap(functions::exchangeToken)
            .flatMap(altinntoken -> functions.sendToAltinnWithDigdirToken(exchange, chain, altinntoken));

    }

}
