package no.difi.meldingsutveksling.dph.client.internal;

import no.difi.meldingsutveksling.dph.client.DphException;
import no.difi.meldingsutveksling.nhn.adapter.model.ApiError;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

public class DphClientErrorHandlerImpl implements DphClientErrorHandler {
    @Override
    public Mono<DphException> apply(ClientResponse response) {
        return response.createException()
            .flatMap(ex -> {
                ApiError apiError = ex.getResponseBodyAs(ApiError.class);

                DphException exception = (apiError != null)
                    ? new DphException(apiError.getErrorCode(), apiError.getMessage())
                    : new DphException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL);

                return Mono.error(exception);
            });
    }
}
