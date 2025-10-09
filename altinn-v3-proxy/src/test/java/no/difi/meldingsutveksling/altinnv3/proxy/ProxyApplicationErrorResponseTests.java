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
 * Tester for authn/authz inn mot selve proxy applikasjonen.
 * <br/>
 * Det er responser som oppstår i spring security laget, ved at man mangler token, har feil scope
 * eller ikke ligger i tilgangslisten for å benytte proxy'en og lignende.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ProxyApplicationErrorResponseTests {

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
        Mockito.when(altinnFunctions.setDigdirTokenInHeaders(any(),any(),any())).thenReturn(Mono.empty());
    }

    @Test
    void whenUnknownPath_thenUnauthorized() {
        webTestClient.get().uri("/no/such/api/path")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$.detail").isEqualTo("Proxy AuthN error : Not Authenticated")
        ;
    }

    @Test
    void whenNoToken_thenUnauthorized() {
        webTestClient.get().uri(CORRESPONDENCE_API_PATH)
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$.detail").isEqualTo("Proxy AuthN error : Not Authenticated")
        ;
    }

    @Test
    void whenUserWithoutScope_thenForbidden() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
            .get().uri(CORRESPONDENCE_API_PATH)
            .exchange()
            .expectStatus().isForbidden()
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$.detail").isEqualTo("Proxy AuthZ error : Access Denied")
        ;
    }

    @Test
    void whenUserWithWrongScope_thenForbidden() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:illegal.scope")))
            .get().uri(CORRESPONDENCE_API_PATH)
            .exchange()
            .expectStatus().isForbidden()
            .expectHeader().contentType("application/json")
            .expectBody()
            .jsonPath("$.detail").isEqualTo("Proxy AuthZ error : Access Denied")
        ;
    }

}
