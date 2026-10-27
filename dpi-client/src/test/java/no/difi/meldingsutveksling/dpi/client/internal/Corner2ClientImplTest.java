package no.difi.meldingsutveksling.dpi.client.internal;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import no.difi.meldingsutveksling.dpi.client.domain.GetMessagesInput;
import no.difi.meldingsutveksling.dpi.client.internal.domain.SendMessageInput;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Verifies that {@link Corner2ClientImpl} bounds every blocking/reactive DPI call with
 * {@code responseTimeout}, so a slow or unresponsive server fails fast instead of hanging the calling
 * thread forever (possibly root cause behind a previously observed permanent stall of polling jobs).
 */
class Corner2ClientImplTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    // Well below SERVER_DELAY_MILLIS, so every test below is guaranteed to hit the timeout rather than the response.
    private static final Duration RESPONSE_TIMEOUT = Duration.ofMillis(300);
    private static final int SERVER_DELAY_MILLIS = 5_000;
    // Generous upper bound used to prove the call fails fast rather than hanging for SERVER_DELAY_MILLIS (or forever).
    private static final Duration MAX_TEST_DURATION = Duration.ofSeconds(2);

    private Corner2ClientImpl corner2Client;

    @BeforeEach
    void beforeEach() {
        WebClient webClient = WebClient.builder()
                .baseUrl(wireMock.baseUrl())
                .build();

        corner2Client = new Corner2ClientImpl(
                webClient,
                new DpiClientErrorHandlerImpl(),
                new CreateMaskinportenTokenMock("DummyMaskinportenToken"),
                new CreateMultipart(),
                InMemoryWithTempFileFallbackResourceFactory.builder().build(),
                10,
                RESPONSE_TIMEOUT);
    }

    @Test
    void getMessageStatuses_timesOutInsteadOfHangingOnSlowServer() {
        UUID messageId = UUID.randomUUID();
        wireMock.stubFor(get(urlPathEqualTo("/messages/out/%s/statuses".formatted(messageId)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(SERVER_DELAY_MILLIS)));

        StepVerifier.create(corner2Client.getMessageStatuses(messageId))
                .expectError(TimeoutException.class)
                .verify(MAX_TEST_DURATION);
    }

    @Test
    void getMessages_timesOutInsteadOfHangingOnSlowServer() {
        wireMock.stubFor(get(urlPathEqualTo("/messages/in"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(SERVER_DELAY_MILLIS)));

        StepVerifier.create(corner2Client.getMessages(new GetMessagesInput()))
                .expectError(TimeoutException.class)
                .verify(MAX_TEST_DURATION);
    }

    @Test
    void sendMessage_timesOutInsteadOfHangingOnSlowServer() {
        wireMock.stubFor(post(urlPathEqualTo("/messages/out"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(SERVER_DELAY_MILLIS)));

        SendMessageInput input = new SendMessageInput()
                .setMaskinportentoken("token")
                .setJwt("jwt")
                .setCmsEncryptedAsice(new ByteArrayResource("asic".getBytes()));

        assertTimesOutQuickly(() -> corner2Client.sendMessage(input));
    }

    @Test
    void markAsRead_timesOutInsteadOfHangingOnSlowServer() {
        UUID messageId = UUID.randomUUID();
        wireMock.stubFor(post(urlPathEqualTo("/messages/in/%s/read".formatted(messageId)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(SERVER_DELAY_MILLIS)));

        assertTimesOutQuickly(() -> corner2Client.markAsRead(messageId));
    }

    @Test
    void getCmsEncryptedAsice_timesOutInsteadOfHangingOnSlowServer() {
        String path = "/downloadmessage/%s".formatted(UUID.randomUUID());
        wireMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("payload")
                        .withFixedDelay(SERVER_DELAY_MILLIS)));

        assertTimesOutQuickly(() -> corner2Client.getCmsEncryptedAsice(URI.create(wireMock.baseUrl() + path)));
    }

    /**
     * Runs a blocking call and asserts it fails (with a timeout somewhere in the cause chain) well within
     * MAX_TEST_DURATION, proving the calling thread is never left hanging on an unresponsive server.
     */
    private static void assertTimesOutQuickly(Runnable blockingCall) {
        long start = System.nanoTime();
        Throwable thrown = catchThrowable(blockingCall::run);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(thrown).as("expected the call to fail due to the response timeout").isNotNull();
        assertThat(elapsed)
                .as("expected the call to fail fast instead of waiting out the slow server")
                .isLessThan(MAX_TEST_DURATION);
        assertThat(hasTimeoutCause(thrown))
                .as("expected a timeout somewhere in the cause chain, but got: %s", thrown)
                .isTrue();
    }

    private static boolean hasTimeoutCause(Throwable throwable) {
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            if (t instanceof TimeoutException || t instanceof IllegalStateException) {
                return true;
            }
        }
        return false;
    }

}
