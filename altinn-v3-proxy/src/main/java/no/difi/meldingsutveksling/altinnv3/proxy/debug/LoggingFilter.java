package no.difi.meldingsutveksling.altinnv3.proxy.debug;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@RequiredArgsConstructor
public class LoggingFilter implements GatewayFilter {

    private final MeterRegistry meterRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        dumpHeaders(exchange.getRequest());
        return chain.filter(exchange).then(
            Mono.fromRunnable( () -> {
                URI routedUrl = exchange.getAttribute("org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRequestUrl");
                log.info("Request path : {}", exchange.getRequest().getPath().value());
                log.info("Destination URL : {}", routedUrl);
                meterRegistry.counter("eformidling.dpv.proxy.total", "method", exchange.getRequest().getMethod().name()).increment();
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
