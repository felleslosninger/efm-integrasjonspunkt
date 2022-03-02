package no.difi.meldingsutveksling.dpi.client.internal;

import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public interface DpiClientErrorHandler extends Function<ClientResponse, Mono<? extends Throwable>> {

}
