package no.difi.meldingsutveksling.altinnv3.proxy;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ProxyApplicationTests {

    static String ACTUATOR_INFO_PATH = "/info";
    static String ACTUATOR_HEALTH_PATH = "/health";
    static String ACTUATOR_LIVENESS_PATH = "/health/liveness";
    static String ACTUATOR_READINESS_PATH = "/health/readiness";
    static String ACTUATOR_METRICS_PATH = "/metrics";
    static String CORRESPONDENCE_API_PATH = "/correspondence/api/v1";

    @Value("${local.management.port}")
    int managementPort;

    @Inject
    private WebTestClient webTestClient;

    private WebTestClient mgmtTestClient;

    @MockitoBean
    private AltinnFunctions altinnFunctions;

    @PostConstruct
    public void initManagementTestWebClient() {
        mgmtTestClient = WebTestClient
            .bindToServer()
            .baseUrl("http://localhost:" + managementPort)
            .build();
    }

    @BeforeEach
    void setupMock() {
        Mockito.when(altinnFunctions.getAccessListToken()).thenReturn(Mono.just("mp-token"));
        Mockito.when(altinnFunctions.getAccessList(any())).thenReturn(Mono.just(List.of("token")));
        Mockito.when(altinnFunctions.isOrgOnAccessList(any(), any())).thenReturn(Mono.empty());
        Mockito.when(altinnFunctions.getCorrespondenceToken()).thenReturn(Mono.just("token"));
        Mockito.when(altinnFunctions.setDigdirTokenInHeaders(any(),any(),any())).thenReturn(Mono.empty());
    }

    @Test
    void shouldRouteToInfoEndpoint() {
        mgmtTestClient.get()
            .uri(ACTUATOR_INFO_PATH)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("application/vnd.spring-boot.actuator.v3+json")
            .expectBody(String.class).consumeWith(s -> {
                s.getResponseBody().startsWith("{");
            });
    }

    @Test
    void shouldRouteToHealthEndpoint() {
        mgmtTestClient.get()
            .uri(ACTUATOR_HEALTH_PATH)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void shouldRouteToLivenessEndpoint() {
        mgmtTestClient.get()
            .uri(ACTUATOR_LIVENESS_PATH)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void shouldRouteToReadinessEndpoint() {
        mgmtTestClient.get()
            .uri(ACTUATOR_READINESS_PATH)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void shouldRouteToMetricsEndpoint() {
        mgmtTestClient.get()
            .uri(ACTUATOR_METRICS_PATH)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).consumeWith(s -> {
                s.getResponseBody().startsWith("{\"names\":[");
            });
    }

    @Test
    void whenUnknownPath_thenUnauthorized() {
        webTestClient.get().uri("/no/such/api/path")
            .exchange()
            .expectStatus().isUnauthorized();
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
            .expectStatus().isOk();
    }

}
