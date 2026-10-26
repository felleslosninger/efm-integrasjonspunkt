package no.difi.meldingsutveksling.dph.client.internal;

import no.difi.meldingsutveksling.dph.client.DphException;
import no.difi.meldingsutveksling.nhn.adapter.model.ApiError;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

public class DphClientErrorHandlerImpl implements DphClientErrorHandler {
    @Override
    public Mono<DphException> apply(ClientResponse response) {
        return response.createException()
            .flatMap(ex -> {
                ApiError apiError = getApiError(ex);

                DphException exception = (apiError != null)
                    ? new DphException(apiError.getErrorCode(), apiError.getMessage(), response.statusCode().value(), ex)
                    : new DphException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL.getVerdi(), ex.getMessage(), response.statusCode().value(), ex);

                return Mono.error(exception);
            });
    }

    private static ApiError getApiError(WebClientResponseException ex) {
        try {
            return ex.getResponseBodyAs(ApiError.class);
        } catch (RuntimeException e) {
            // Body is not an ApiError, the original response is kept as cause instead
            return null;
        }
    }
}
