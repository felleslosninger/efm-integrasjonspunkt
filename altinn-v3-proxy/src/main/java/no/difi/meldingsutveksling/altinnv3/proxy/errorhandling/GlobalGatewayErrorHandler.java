package no.difi.meldingsutveksling.altinnv3.proxy.errorhandling;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static no.difi.meldingsutveksling.altinnv3.proxy.errorhandling.ProblemDetails.createProblemDetailsAsByteArray;

@Component
@Order(-2)  // must be lower than DefaultErrorWebExceptionHandler
public class GlobalGatewayErrorHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        var status = HttpStatus.INTERNAL_SERVER_ERROR;
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(createProblemDetailsAsByteArray(status.name(), status.value(), "Proxy Gateway Error : " + ex.getMessage())))
        );
    }

}
