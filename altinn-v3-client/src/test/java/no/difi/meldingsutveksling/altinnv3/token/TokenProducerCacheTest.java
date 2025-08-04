package no.difi.meldingsutveksling.altinnv3.token;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.config.AltinnFormidlingsTjenestenConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.PostVirksomheter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * This test enables caching and when calling produceToken() three times with the same
 * parameters it expects the cache to kick in and verifies that both the TokenService
 * and TokenExchangeService have only been called once each.
 */
@SpringBootTest
class TokenProducerCacheTest {

    @Inject
    TokenProducer dpoTokenProducer;

    @Inject
    TokenProducer dpvTokenProducer;

    @EnableCaching
    @Configuration
    static class TestConfig {

        public static TokenService tokenService = mock(TokenService.class);
        public static TokenExchangeService tokenExchangeService = mock(TokenExchangeService.class);

        @Bean
        public DpoTokenProducer dpoTokenProducer() {
            var dpo = new AltinnFormidlingsTjenestenConfig();
            var props = mock(IntegrasjonspunktProperties.class);
            when(props.getDpo()).thenReturn(dpo);
            return new DpoTokenProducer(props, tokenService, tokenExchangeService);
        }

        @Bean
        public DpvTokenProducer dpvTokenProducer() {
            var dpv = new PostVirksomheter();
            var props = mock(IntegrasjonspunktProperties.class);
            when(props.getDpv()).thenReturn(dpv);
            return new DpvTokenProducer(props, tokenService, tokenExchangeService);
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("altinn.getDpoToken", "altinn.getDpvToken");
        }

    }

    @BeforeEach
    void setUp() {
        when(TestConfig.tokenService.fetchToken(any(), any())).thenReturn("maskinportentoken");
        when(TestConfig.tokenExchangeService.exchangeToken(any(), any())).thenReturn("altinntoken");
    }

    @Test
    void produceDpoToken() {
        Mockito.clearInvocations(TestConfig.tokenService);
        Mockito.clearInvocations(TestConfig.tokenExchangeService);
        var scopes = List.of("scopes");
        var token = dpoTokenProducer.produceToken(scopes);
        dpoTokenProducer.produceToken(scopes);
        dpoTokenProducer.produceToken(scopes);
        assertEquals("altinntoken", token);
        verify(TestConfig.tokenService, times(1)).fetchToken(any(), any());
        verify(TestConfig.tokenExchangeService, times(1)).exchangeToken(any(), any());
    }

    @Test
    void produceDpvToken() {
        Mockito.clearInvocations(TestConfig.tokenService);
        Mockito.clearInvocations(TestConfig.tokenExchangeService);
        var scopes = List.of("scopes");
        var token = dpvTokenProducer.produceToken(scopes);
        dpvTokenProducer.produceToken(scopes);
        dpvTokenProducer.produceToken(scopes);
        assertEquals("altinntoken", token);
        verify(TestConfig.tokenService, times(1)).fetchToken(any(), any());
        verify(TestConfig.tokenExchangeService, times(1)).exchangeToken(any(), any());
    }

}
