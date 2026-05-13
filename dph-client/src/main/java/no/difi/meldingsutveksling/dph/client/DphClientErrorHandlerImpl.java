package no.difi.meldingsutveksling.dph.client;

import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

public class DphClientErrorHandlerImpl implements DphClientErrorHandler {
    @Override
    public Mono<DphException> apply(ClientResponse response) {
        return response.createException()
            .flatMap(ex -> Mono.error(new DphException(
                "%s:%n%s".formatted(ex.getMessage(), ex.getResponseBodyAsString()))));
    }
}
