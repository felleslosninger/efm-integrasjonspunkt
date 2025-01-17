package no.difi.meldingsutveksling.dpi.client.internal;

import no.difi.meldingsutveksling.dpi.client.Blame;
import no.difi.meldingsutveksling.dpi.client.DpiException;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

public class DpiClientErrorHandlerImpl implements DpiClientErrorHandler {
    @Override
    public Mono<DpiException> apply(ClientResponse response) {
        return response.createException()
                .flatMap(ex -> Mono.error(new DpiException(
                "%s:%n%s".formatted(ex.getMessage(), ex.getResponseBodyAsString()),
                        getBlame(response))));
    }

    private Blame getBlame(ClientResponse response) {
        return response.statusCode().is5xxServerError() ? Blame.SERVER : Blame.CLIENT;
    }
}
