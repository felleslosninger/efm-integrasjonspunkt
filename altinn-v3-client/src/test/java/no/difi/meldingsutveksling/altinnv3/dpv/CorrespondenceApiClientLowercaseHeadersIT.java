package no.difi.meldingsutveksling.altinnv3.dpv;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import no.difi.meldingsutveksling.altinnv3.token.TokenProducer;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.PostVirksomheter;
import no.digdir.altinn3.correspondence.model.CorrespondenceStatusExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression guard: efm-dpv-proxy forwards Altinn Correspondence API response headers unchanged,
 * so they arrive with all-lowercase names (e.g. {@code content-type}). {@link CorrespondenceApiClient}
 * must handle this, since HTTP header names are case-insensitive.
 * <p>
 * WireMock's Jetty canonicalises well-known header names on the wire ({@code content-type} becomes
 * {@code Content-Type}), so the client talks to WireMock through {@link LowercaseHeaderProxy}, which
 * lowercases every response header name before it reaches the client.
 */
@Isolated
class CorrespondenceApiClientLowercaseHeadersIT {

    private static final String TOKEN = "test-token";
    private static final String CORRESPONDENCE_BASE_PATH = "/correspondence/api/v1";
    private static final String HEALTH_PATH = "/health";

    private WireMockServer wireMockServer;
    private LowercaseHeaderProxy proxy;
    private CorrespondenceApiClient client;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort().http2PlainDisabled(true));
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        proxy = new LowercaseHeaderProxy(wireMockServer.port());

        var tokenProducer = mock(TokenProducer.class);
        when(tokenProducer.produceToken(anyList())).thenReturn(TOKEN);

        var dpvConfig = new PostVirksomheter();
        dpvConfig.setCorrespondenceServiceUrl(proxy.baseUrl() + CORRESPONDENCE_BASE_PATH);
        dpvConfig.setHealthCheckUrl(proxy.baseUrl() + HEALTH_PATH);
        var props = mock(IntegrasjonspunktProperties.class);
        when(props.getDpv()).thenReturn(dpvConfig);

        client = new CorrespondenceApiClient(tokenProducer, new DotNotationFlattener(), props);
        client.init();
    }

    @AfterEach
    void tearDown() throws IOException {
        proxy.close();
        wireMockServer.stop();
    }

    @Test
    void stubsReallySendLowercaseHeaderNamesOnTheWire() throws IOException {
        wireMockServer.stubFor(get(urlEqualTo(HEALTH_PATH))
            .willReturn(lowercaseResponse(200, "application/json", "{}")));

        var headerNames = rawResponseHeaderNames(HEALTH_PATH);

        assertTrue(headerNames.containsAll(List.of("content-type", "content-length", "date")), "Got " + headerNames);
        assertTrue(headerNames.stream().allMatch(name -> name.equals(name.toLowerCase(Locale.ROOT))), "Got " + headerNames);
    }

    @Test
    void getCorrespondenceOverviewDeserializesJsonWithLowercaseContentType() {
        assertOverviewDeserialized("application/json");
    }

    @Test
    void getCorrespondenceOverviewDeserializesJsonWithLowercaseContentTypeAndCharset() {
        assertOverviewDeserialized("application/json; charset=utf-8");
    }

    @Test
    void downloadAttachmentReturnsBytesWithLowercaseContentType() {
        var correspondenceId = UUID.randomUUID();
        var attachmentId = UUID.randomUUID();
        var bytes = new byte[]{0x50, 0x4b, 0x03, 0x04, 0, 1, 2, (byte) 0xff};
        var path = CORRESPONDENCE_BASE_PATH + "/correspondence/" + correspondenceId + "/attachment/" + attachmentId + "/download";
        wireMockServer.stubFor(get(urlEqualTo(path))
            .willReturn(lowercaseResponse(200, "application/octet-stream", bytes)));

        assertArrayEquals(bytes, client.downloadAttachment(correspondenceId, attachmentId));
    }

    @Test
    void errorWithLowercaseProblemJsonContentTypeThrowsCorrespondenceApiException() {
        var correspondenceId = UUID.randomUUID();
        var path = CORRESPONDENCE_BASE_PATH + "/correspondence/" + correspondenceId;
        var problem = "{\"type\":\"https://tools.ietf.org/html/rfc9110#section-15.5.5\",\"title\":\"Not Found\",\"status\":404,\"detail\":\"Correspondence not found\"}";
        wireMockServer.stubFor(get(urlEqualTo(path))
            .willReturn(lowercaseResponse(404, "application/problem+json", problem)));

        var exception = assertThrows(CorrespondenceApiException.class, () -> client.getCorrespondenceOverview(correspondenceId));

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getMessage().contains("Correspondence not found"), exception.getMessage());
        assertTrue(exception.getMessage().contains(path), exception.getMessage());
    }

    @Test
    void badRequestWithLowercaseProblemJsonContentTypeThrowsCorrespondenceApiException() {
        var path = CORRESPONDENCE_BASE_PATH + "/correspondence?sendersReference=ref-1";
        var problem = "{\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Invalid sendersReference\"}";
        wireMockServer.stubFor(get(urlEqualTo(path))
            .willReturn(lowercaseResponse(400, "application/problem+json", problem)));

        var exception = assertThrows(CorrespondenceApiException.class, () -> client.findCorrespondences("ref-1"));

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getMessage().contains("Invalid sendersReference"), exception.getMessage());
    }

    @Test
    void connectionTestSucceedsWithLowercaseHeaders() {
        wireMockServer.stubFor(get(urlEqualTo(HEALTH_PATH))
            .willReturn(lowercaseResponse(200, "application/json", "{\"status\":\"Healthy\"}")));

        client.connectionTest();

        wireMockServer.verify(getRequestedFor(urlEqualTo(HEALTH_PATH)));
    }

    private void assertOverviewDeserialized(String contentType) {
        var correspondenceId = UUID.randomUUID();
        var path = CORRESPONDENCE_BASE_PATH + "/correspondence/" + correspondenceId;
        wireMockServer.stubFor(get(urlEqualTo(path))
            .willReturn(lowercaseResponse(200, contentType, """
                {
                  "correspondenceId": "%s",
                  "status": "Published",
                  "resourceId": "eformidling-dpv",
                  "sendersReference": "ref-1",
                  "recipient": "0192:222222222"
                }
                """.formatted(correspondenceId))));

        var overview = client.getCorrespondenceOverview(correspondenceId);

        assertEquals(correspondenceId, overview.getCorrespondenceId());
        assertEquals(CorrespondenceStatusExt.PUBLISHED, overview.getStatus());
        assertEquals("eformidling-dpv", overview.getResourceId());
        assertEquals("ref-1", overview.getSendersReference());
        assertEquals("0192:222222222", overview.getRecipient());
        wireMockServer.verify(getRequestedFor(urlEqualTo(path))
            .withHeader("Authorization", equalTo("Bearer " + TOKEN)));
    }

    private static ResponseDefinitionBuilder lowercaseResponse(int status, String contentType, String body) {
        return lowercaseResponse(status, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private static ResponseDefinitionBuilder lowercaseResponse(int status, String contentType, byte[] body) {
        return aResponse()
            .withStatus(status)
            .withBody(body)
            .withHeader("content-type", contentType)
            .withHeader("content-length", String.valueOf(body.length))
            .withHeader("date", "Tue, 20 Oct 2026 10:00:00 GMT")
            .withHeader("x-correlation-id", UUID.randomUUID().toString())
            // one response per connection, so the proxy only has to rewrite a single response head
            .withHeader("connection", "close");
    }

    /**
     * Reads the response header names exactly as sent on the wire, to prove the client
     * really receives lowercase names.
     */
    private List<String> rawResponseHeaderNames(String path) throws IOException {
        try (var socket = new Socket("localhost", proxy.port())) {
            OutputStream out = socket.getOutputStream();
            out.write(("GET " + path + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            reader.readLine(); // status line
            var names = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                names.add(line.substring(0, line.indexOf(':')));
            }
            return names;
        }
    }

    /**
     * Minimal TCP relay in front of WireMock: forwards requests unchanged and lowercases every
     * header name in the response head, mimicking efm-dpv-proxy passing Altinn's headers through.
     * Relies on the stubs sending {@code connection: close} (one response per connection).
     */
    private static class LowercaseHeaderProxy implements Closeable {

        private final int targetPort;
        private final ServerSocket serverSocket;
        private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
            var thread = new Thread(runnable, "lowercase-header-proxy");
            thread.setDaemon(true);
            return thread;
        });

        LowercaseHeaderProxy(int targetPort) {
            this.targetPort = targetPort;
            try {
                this.serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            executor.execute(this::acceptLoop);
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        String baseUrl() {
            return "http://localhost:" + port();
        }

        private void acceptLoop() {
            while (!serverSocket.isClosed()) {
                try {
                    var client = serverSocket.accept();
                    executor.execute(() -> relay(client));
                } catch (IOException e) {
                    // closed
                }
            }
        }

        private void relay(Socket client) {
            try (client; var upstream = new Socket(InetAddress.getLoopbackAddress(), targetPort)) {
                executor.execute(() -> {
                    try {
                        client.getInputStream().transferTo(upstream.getOutputStream());
                    } catch (IOException e) {
                        // connection closed
                    }
                });
                var in = upstream.getInputStream();
                var out = client.getOutputStream();
                out.write(lowercaseHeaderNames(readHead(in)).getBytes(StandardCharsets.ISO_8859_1));
                in.transferTo(out);
                out.flush();
            } catch (IOException e) {
                // connection closed
            }
        }

        private static String readHead(InputStream in) throws IOException {
            var head = new StringBuilder();
            int b;
            while ((b = in.read()) != -1) {
                head.append((char) b);
                if (head.length() >= 4 && head.substring(head.length() - 4).equals("\r\n\r\n")) {
                    break;
                }
            }
            return head.toString();
        }

        private static String lowercaseHeaderNames(String head) {
            var lines = head.split("\r\n", -1);
            for (int i = 1; i < lines.length; i++) {
                int colon = lines[i].indexOf(':');
                if (colon > 0) {
                    lines[i] = lines[i].substring(0, colon).toLowerCase(Locale.ROOT) + lines[i].substring(colon);
                }
            }
            return String.join("\r\n", lines);
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
            executor.shutdownNow();
        }
    }

}
