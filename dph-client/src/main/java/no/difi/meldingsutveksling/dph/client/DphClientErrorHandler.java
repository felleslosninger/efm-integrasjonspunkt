package no.difi.meldingsutveksling.dph.client;

import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public interface DphClientErrorHandler extends Function<ClientResponse, Mono<? extends Throwable>> {

}
