package no.difi.meldingsutveksling.altinnv3.proxy;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)  // must be lower than DefaultErrorWebExceptionHandler
public class GlobalGatewayErrorHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        var status = HttpStatus.INTERNAL_SERVER_ERROR;
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        var problemDetails = """
        {
            "type":"https://tools.ietf.org/html/rfc9110",
            "title":"%s",
            "status":%d,
            "detail":"%s"
        }
        """.formatted(status.name(), status.value(), "GlobalGatewayErrorHandler : " + ex.getMessage());
        System.out.println(problemDetails);

        byte[] bytes = problemDetails.getBytes();
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }

}
