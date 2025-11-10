package no.difi.meldingsutveksling.altinnv3.proxy;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureObservability
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProxyApplicationMetricsTests {

    static String ACTUATOR_METRICS_PATH = "/prometheus";
    static String CORRESPONDENCE_API_PATH = "/correspondence/api/v1";
    static String PING_PATH = "/ping";
    static String SCOPE_DPV = "eformidling:dpv";

    @Value("${local.management.port}")
    int managementPort;

    @Inject
    private WebTestClient webTestClient;

    private WebTestClient mgmtTestClient;

    @PostConstruct
    public void initManagementTestWebClient() {
        mgmtTestClient = WebTestClient
            .bindToServer()
            .baseUrl("http://localhost:" + managementPort)
            .build();
    }

    @Test
    @Order(1)
    void verifyNoMetricsBeforeCall() {
        mgmtTestClient.get()
            .uri(ACTUATOR_METRICS_PATH)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).consumeWith(s -> {
                assertTrue(s.getResponseBody().startsWith("# HELP "));
            });
    }

    @Test
    @Order(2)
    void oneCallToGeneratePingMetric() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", SCOPE_DPV)))
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

    @Disabled
    @Test
    @Order(3)
    void oneCallToGeneratForwardMetric() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", SCOPE_DPV)))
            .get().uri(CORRESPONDENCE_API_PATH)
            .exchange()
            .expectStatus().isOk();
    }

    @Disabled
    @Test
    @Order(4)
    void oneCallToGeneratForwardMetricButTokenShouldBeCached() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt.claim("scope", SCOPE_DPV)))
            .get().uri(CORRESPONDENCE_API_PATH)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @Order(5)
    void verifyMetricsAfterCall() {
        mgmtTestClient.get()
            .uri(ACTUATOR_METRICS_PATH)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).consumeWith(s -> {
                var prometheusMetrics = s.getResponseBody();
                assertTrue(prometheusMetrics.startsWith("# HELP "));
                assertTrue(prometheusMetrics.contains("""
                    eformidling_dpv_proxy_request_total{method="GET",type="ping"} 1.0"""));
            });
    }

}
