package no.difi.meldingsutveksling.nextmove.servicebus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;
import java.util.Optional;

import static no.difi.meldingsutveksling.config.CacheConfig.CACHE_GET_SAS_KEY;

@Component
@Slf4j
@RequiredArgsConstructor
public class ServiceBusRestErrorHandler extends DefaultResponseErrorHandler {

    private final CacheManager cacheManager;

    @Override
    public void handleError(@NotNull ClientHttpResponse response) throws IOException {
        if (HttpStatus.UNAUTHORIZED.isSameCodeAs(response.getStatusCode())) {
            log.debug("Got status {} from service bus, invalidating sas key", HttpStatus.valueOf(response.getStatusCode().value()).getReasonPhrase());
            Optional.ofNullable(cacheManager.getCache(CACHE_GET_SAS_KEY))
                    .orElseThrow(() -> new MeldingsUtvekslingRuntimeException(
                            "Couldn't get cache names %s".formatted(CACHE_GET_SAS_KEY)))
                    .clear();
        }
        super.handleError(response);
    }

}
