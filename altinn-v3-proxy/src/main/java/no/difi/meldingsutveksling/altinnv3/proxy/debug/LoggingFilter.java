package no.difi.meldingsutveksling.altinnv3.proxy.debug;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
public class LoggingFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        dumpHeaders(exchange.getRequest());
        return chain.filter(exchange).then(
            Mono.fromRunnable( () -> {
                URI routedUrl = exchange.getAttribute("org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRequestUrl");
                log.debug("Destination URL : {}", routedUrl);
            } )
        );
    }

    void dumpHeaders(ServerHttpRequest request) {
        log.debug("Request URL : {}", request.getURI());
        request.getHeaders().forEach((name, values) -> {
            log.debug("Header : " + name + " = " + values);
        });
    }

}
