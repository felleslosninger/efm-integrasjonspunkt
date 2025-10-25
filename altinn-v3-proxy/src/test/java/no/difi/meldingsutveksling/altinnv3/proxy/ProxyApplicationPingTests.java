package no.difi.meldingsutveksling.altinnv3.proxy;

import com.jayway.jsonpath.JsonPath;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ProxyApplicationPingTests {

    @Inject
    private WebTestClient webTestClient;

    static String PING_PATH = "/ping";

    @Test
    void whenUnknownPath_thenUnauthorized() {
        webTestClient.get().uri("/no/such/api/path")
            .exchange()
            .expectStatus().isUnauthorized()
        ;
    }

    @Test
    void whenNoToken_thenUnauthorized() {
        webTestClient.get().uri(PING_PATH)
            .exchange()
            .expectStatus().isUnauthorized()
        ;
    }

    @Test
    void whenUserWithoutScope_thenForbidden() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
            .get().uri(PING_PATH)
            .exchange()
            .expectStatus().isForbidden()
        ;
    }

    @Test
    void whenUserWithWrongScope_thenForbidden() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:illegal.scope")))
            .get().uri(PING_PATH)
            .exchange()
            .expectStatus().isForbidden()
        ;
    }

    @Test
    void whenValidJwtButPathOverload_thenForbidden() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:broker.read")))
            .get().uri(PING_PATH + "/unexpected")
            .exchange()
            .expectStatus().isForbidden()
        ;
    }

    @Test
    void whenValidJwt_thenAuthorized() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:broker.read")))
            .get().uri(PING_PATH)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("ok")
            .jsonPath("$.message").isEqualTo("pong")
            .jsonPath("$.timestamp").isNotEmpty()
            .jsonPath("$.waited").isEqualTo("0 ms")
        ;
    }

    @Test
    void whenValidJwt_thenAuthorizedWithSizeTooLow() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:broker.read")))
            .get().uri(PING_PATH + "?size=0")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("ok")
            .jsonPath("$.message").isEqualTo("Size was 0, must be between 1 and 1 MiB")
            .jsonPath("$.timestamp").isNotEmpty()
            .jsonPath("$.waited").isEqualTo("0 ms")
        ;
    }

    @Test
    void whenValidJwt_thenAuthorizedWithSizeWayTooLow() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:broker.read")))
            .get().uri(PING_PATH + "?size=-100")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("ok")
            .jsonPath("$.message").isEqualTo("Size was -100, must be between 1 and 1 MiB")
            .jsonPath("$.timestamp").isNotEmpty()
            .jsonPath("$.waited").isEqualTo("0 ms")
        ;
    }

    @Test
    void whenValidJwt_thenAuthorizedWithSizeTooHigh() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:broker.read")))
            .get().uri(PING_PATH + "?size=987654321")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("ok")
            .jsonPath("$.message").isEqualTo("Size was 987654321, must be between 1 and 1 MiB")
            .jsonPath("$.timestamp").isNotEmpty()
            .jsonPath("$.waited").isEqualTo("0 ms")
        ;
    }

    @Test
    void whenValidJwt_thenAuthorizedWithSizeCorrect() {
        int EXPECTED_SIZE = 131072;
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:broker.read")))
            .get().uri(PING_PATH + "?size=" + EXPECTED_SIZE)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .consumeWith(response -> {
                String message = JsonPath.read(response.getResponseBody(), "$.message");
                assertEquals(EXPECTED_SIZE, message.length());
            });
    }

    @Test
    void whenValidJwt_thenAuthorizedAWithWaitTooLow() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:broker.read")))
            .get().uri(PING_PATH + "?wait=-100")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("ok")
            .jsonPath("$.message").isEqualTo("pong")
            .jsonPath("$.timestamp").isNotEmpty()
            .jsonPath("$.waited").isEqualTo("0 ms")
        ;
    }

    @Test
    void whenValidJwt_thenAuthorizedAWithWaitTooHigh() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:broker.read")))
            .get().uri(PING_PATH + "?wait=60000")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("ok")
            .jsonPath("$.message").isEqualTo("pong")
            .jsonPath("$.timestamp").isNotEmpty()
            .jsonPath("$.waited").isEqualTo("3000 ms")
        ;
    }

    @Test
    void whenValidJwt_thenAuthorizedWithWaitForThreeSeconds() {
        long start = System.currentTimeMillis();
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", "altinn:broker.read")))
            .get().uri(PING_PATH + "?wait=3000")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("ok")
            .jsonPath("$.message").isEqualTo("pong")
            .jsonPath("$.timestamp").isNotEmpty()
            .jsonPath("$.waited").isEqualTo("3000 ms")
        ;
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 3_000L, "The request was expected to wait at least 3 seconds");
    }

}
