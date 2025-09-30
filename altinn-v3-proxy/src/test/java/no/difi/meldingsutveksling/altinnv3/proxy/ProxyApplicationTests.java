package no.difi.meldingsutveksling.altinnv3.proxy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ProxyApplicationTests {

    static String ACTUATOR_HEALTH_PATH = "/actuator/health";
    static String CORRESPONDENCE_API_PATH = "/correspondence/api/v1";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldRouteToHealthEndpoint() {
        webTestClient.get()
            .uri(ACTUATOR_HEALTH_PATH)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).isEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    void whenNoToken_thenUnauthorized() {
        webTestClient.get().uri(CORRESPONDENCE_API_PATH)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void whenUserWithoutScope_thenForbidden() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
            .get().uri(CORRESPONDENCE_API_PATH)
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void whenUserWithWrongScope_thenForbidden() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:illegal.scope")))
            .get().uri(CORRESPONDENCE_API_PATH)
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void whenValidJwt_thenAuthorized() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:broker.read")))
            .get().uri(CORRESPONDENCE_API_PATH)
            .exchange()
            .expectStatus().is5xxServerError(); // FIXME this is NOT we would expect, but at least we get past the security layer
    }

}
