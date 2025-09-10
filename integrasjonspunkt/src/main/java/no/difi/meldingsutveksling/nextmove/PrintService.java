package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.CacheConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PrintService {

    private final IntegrasjonspunktProperties properties;
    private WebClient wc;

    @PostConstruct
    public void init() {
        wc = WebClient.create(properties.getDpi().getKrrPrintUrl());
    }

    @Cacheable(CacheConfig.CACHE_KRR_PRINT)
    public KrrPrintResponse getPrintDetails() {
        return wc.get()
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(KrrPrintResponse.class)
            .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(3)))
            .block(Duration.ofSeconds(30));
    }

}
