package no.difi.meldingsutveksling.dpi.client.internal;

import no.difi.meldingsutveksling.dpi.client.domain.GetMessagesInput;
import no.difi.meldingsutveksling.dpi.client.internal.domain.SendMessageInput;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.Delay;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Verifies that {@link Corner2ClientImpl} bounds every blocking/reactive DPI call with
 * {@code responseTimeout}, so a slow or unresponsive server fails fast instead of hanging the calling
 * thread forever (possibly root cause behind a previously observed permanent stall of polling jobs).
 */
@MockServerSettings(ports = 8901)
class Corner2ClientImplTest {

    // Well below SERVER_DELAY, so every test below is guaranteed to hit the timeout rather than the response.
    private static final Duration RESPONSE_TIMEOUT = Duration.ofMillis(300);
    private static final Delay SERVER_DELAY = new Delay(TimeUnit.SECONDS, 5);
    // Generous upper bound used to prove the call fails fast rather than hanging for SERVER_DELAY (or forever).
    private static final Duration MAX_TEST_DURATION = Duration.ofSeconds(2);

    private Corner2ClientImpl corner2Client;

    @BeforeEach
    void beforeEach() {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8901")
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
    void getMessageStatuses_timesOutInsteadOfHangingOnSlowServer(MockServerClient client) {
        UUID messageId = UUID.randomUUID();
        client.when(request()
                        .withMethod("GET")
                        .withPath("/messages/out/%s/statuses".formatted(messageId)))
                .respond(response()
                        .withStatusCode(200)
                        .withDelay(SERVER_DELAY));

        StepVerifier.create(corner2Client.getMessageStatuses(messageId))
                .expectError(TimeoutException.class)
                .verify(MAX_TEST_DURATION);
    }

    @Test
    void getMessages_timesOutInsteadOfHangingOnSlowServer(MockServerClient client) {
        client.when(request()
                        .withMethod("GET")
                        .withPath("/messages/in"))
                .respond(response()
                        .withStatusCode(200)
                        .withDelay(SERVER_DELAY));

        StepVerifier.create(corner2Client.getMessages(new GetMessagesInput()))
                .expectError(TimeoutException.class)
                .verify(MAX_TEST_DURATION);
    }

    @Test
    void sendMessage_timesOutInsteadOfHangingOnSlowServer(MockServerClient client) {
        client.when(request()
                        .withMethod("POST")
                        .withPath("/messages/out"))
                .respond(response()
                        .withStatusCode(200)
                        .withDelay(SERVER_DELAY));

        SendMessageInput input = new SendMessageInput()
                .setMaskinportentoken("token")
                .setJwt("jwt")
                .setCmsEncryptedAsice(new ByteArrayResource("asic".getBytes()));

        assertTimesOutQuickly(() -> corner2Client.sendMessage(input));
    }

    @Test
    void markAsRead_timesOutInsteadOfHangingOnSlowServer(MockServerClient client) {
        UUID messageId = UUID.randomUUID();
        client.when(request()
                        .withMethod("POST")
                        .withPath("/messages/in/%s/read".formatted(messageId)))
                .respond(response()
                        .withStatusCode(200)
                        .withDelay(SERVER_DELAY));

        assertTimesOutQuickly(() -> corner2Client.markAsRead(messageId));
    }

    @Test
    void getCmsEncryptedAsice_timesOutInsteadOfHangingOnSlowServer(MockServerClient client) {
        String path = "/downloadmessage/%s".formatted(UUID.randomUUID());
        client.when(request()
                        .withMethod("GET")
                        .withPath(path))
                .respond(response()
                        .withStatusCode(200)
                        .withBody("payload")
                        .withDelay(SERVER_DELAY));

        assertTimesOutQuickly(() -> corner2Client.getCmsEncryptedAsice(URI.create("http://localhost:8901" + path)));
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
