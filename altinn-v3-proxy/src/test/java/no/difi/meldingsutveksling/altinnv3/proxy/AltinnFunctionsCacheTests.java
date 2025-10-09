package no.difi.meldingsutveksling.altinnv3.proxy;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.proxy.token.TokenProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class AltinnFunctionsCacheTests {

    @Inject AltinnFunctions altinnFunctions;

    @MockitoBean
    private TokenProducer tokenProducer;

    @BeforeEach
    public void setup() {
        altinnFunctions.invalidateCache(); // make sure each test starts with an empty cache
        Mockito.when(tokenProducer.fetchMaskinportenToken(any())).thenAnswer(invocation -> Mono.just("maskinporten-token-" + System.nanoTime()));
        Mockito.when(tokenProducer.exchangeToAltinnToken(any())).thenAnswer(invocation -> Mono.just("altinn-token-" + System.nanoTime()));
    }

    @Test
    public void verifyCaching_forAccessListTokens() {

        var token1 = altinnFunctions.getAccessListToken().block();
        var token2 = altinnFunctions.getAccessListToken().block();

        assertEquals(token1, token2);

        verify(tokenProducer, times(1)).fetchMaskinportenToken(any());
        verify(tokenProducer, times(1)).exchangeToAltinnToken(any());

    }

    @Test
    public void verifyCaching_forCorrespondenceTokens() {

        var token1 = altinnFunctions.getCorrespondenceToken().block();
        var token2 = altinnFunctions.getCorrespondenceToken().block();

        assertEquals(token1, token2);

        verify(tokenProducer, times(1)).fetchMaskinportenToken(any());
        verify(tokenProducer, times(1)).exchangeToAltinnToken(any());

    }

    @Test
    public void verifyCaching_forMultipleTokens() {

        var token1 = altinnFunctions.getAccessListToken().block();
        var token2 = altinnFunctions.getCorrespondenceToken().block();

        assertNotEquals(token1, token2);

        verify(tokenProducer, times(2)).fetchMaskinportenToken(any());
        verify(tokenProducer, times(2)).exchangeToAltinnToken(any());

    }

}
