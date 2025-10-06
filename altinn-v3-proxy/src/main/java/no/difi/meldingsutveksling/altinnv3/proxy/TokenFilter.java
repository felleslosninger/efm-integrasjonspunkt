package no.difi.meldingsutveksling.altinnv3.proxy;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class TokenFilter implements GatewayFilter {

    private final AltinnFunctions functions;

    public TokenFilter(AltinnFunctions functions) {
        this.functions = functions;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return functions.getAccessListToken()
            .flatMap(functions::getAccessList)
            .flatMap(accessListMembers -> functions.isOrgOnAccessList(exchange, accessListMembers))
            .then(functions.getCorrespondenceToken())
            .flatMap(functions::exchangeToAltinnToken)
            .flatMap(altinntoken -> functions.setDigdirTokenInHeaders(exchange, chain, altinntoken))
            .flatMap(chain::filter);
    }
}
