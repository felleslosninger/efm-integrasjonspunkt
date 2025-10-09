package no.difi.meldingsutveksling.altinnv3.proxy;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

/**
 * Tester for error håndtering fra TokenFilter proxy chain.
 * </br>
 * Dette er feil som skjer internt i selve proxy chain filteret, det gir
 * 5xx type responser som returneres til klient,
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ProxyChainErrorHandlingTests {

    static String CORRESPONDENCE_API_PATH = "/correspondence/api/v1";

    @Inject
    private WebTestClient webTestClient;

    @MockitoBean
    private AltinnFunctions altinnFunctions;

    @BeforeEach
    void setupMock() {
        Mockito.when(altinnFunctions.getAccessListToken()).thenReturn(Mono.just("mp-token"));
        Mockito.when(altinnFunctions.getAccessList(any())).thenReturn(Mono.just(List.of("token")));
        Mockito.when(altinnFunctions.isOrgOnAccessList(any(), any())).thenReturn(Mono.empty());
        Mockito.when(altinnFunctions.getCorrespondenceToken()).thenReturn(Mono.just("token"));
        Mockito.when(altinnFunctions.setDigdirTokenInHeaders(any(),any(),any())).thenReturn(Mono.error(new RuntimeException("Mocked Error in the TokenFilter Proxy Chain")));
    }

    @Test
    void whenChainThrowsException_thenExpectProblemDetails() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:broker.read")))
            .get().uri(CORRESPONDENCE_API_PATH)
            .exchange()
            .expectStatus().is5xxServerError()
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$.detail").isEqualTo("Proxy Gateway Error : Mocked Error in the TokenFilter Proxy Chain")
        ;
    }

}
