package no.difi.meldingsutveksling.altinnv3.proxy.errorhandling;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static no.difi.meldingsutveksling.altinnv3.proxy.errorhandling.ProblemDetails.createProblemDetailsAsByteArray;

public class GlobalSpringSecurityErrorHandler implements ServerAccessDeniedHandler, ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException ex) {
        return this.writeProblemDetails(exchange, HttpStatus.FORBIDDEN, "Proxy AuthZ error : " + ex.getMessage());
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return this.writeProblemDetails(exchange, HttpStatus.UNAUTHORIZED, "Proxy AuthN error : " + ex.getMessage());
    }

    private Mono<Void> writeProblemDetails(ServerWebExchange exchange, HttpStatus status, String detail) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(createProblemDetailsAsByteArray(status.name(), status.value(), detail)))
        );
    }

}
