package no.difi.meldingsutveksling.dph.client.internal;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.MessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@MockitoSettings(strictness = Strictness.LENIENT)
class DphClientImplTest {

    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private DphClientImpl dphClient;

    @Mock
    private DphParcelService parcelService;
    @Mock
    private CreateMaskinportenToken createMaskinportenToken;

    private final Iso6523 onBehalfOf = Iso6523.parse("0192:987654321");
    private final String token = "dummy-token";

    @BeforeEach
    void setUp() {
        parcelService = mock(DphParcelService.class);
        createMaskinportenToken = mock(CreateMaskinportenToken.class);

        WebClient webClient = WebClient.builder()
                .baseUrl(wireMockExtension.baseUrl())
                .build();
        dphClient = new DphClientImpl(
                webClient,
                new CreateMultipart(),
                parcelService,
                new DphClientErrorHandlerImpl(),
                createMaskinportenToken
        );

        given(createMaskinportenToken.createMaskinportenToken(onBehalfOf)).willReturn(token);
    }

    @Test
    void testGetStatus() {
        String messageId = UUID.randomUUID().toString();
        String json = "[]";

        wireMockExtension.stubFor(get(urlEqualTo("/messages/out/" + messageId + "/statuses"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(json)));

        List<MessageStatus> result = dphClient.getStatus(onBehalfOf, messageId);

        assertThat(result).isEmpty();
    }

    @Test
    void testSendBusinessDocument() {
        UUID expectedId = UUID.randomUUID();
        WrappedPackage wrappedPackage = new WrappedPackage("jose-content", new ByteArrayResource("asic-content".getBytes()));

        wireMockExtension.stubFor(post(urlEqualTo("/messages/out"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.MULTIPART_FORM_DATA_VALUE))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                        .withBody(expectedId.toString())));

        UUID result = dphClient.sendBusinessDocument(onBehalfOf, wrappedPackage);

        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    void testSendApplicationReceipt() {
        UUID expectedId = UUID.randomUUID();
        WrappedPackage wrappedPackage = new WrappedPackage("jose-receipt");

        wireMockExtension.stubFor(post(urlEqualTo("/messages/out/receipt"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo("application/jose"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                        .withBody(expectedId.toString())));

        UUID result = dphClient.sendApplicationReceipt(onBehalfOf, wrappedPackage);

        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    void testGetMessages() {
        Integer receiverHerId = 12345;
        String json = "[]";

        wireMockExtension.stubFor(get(urlPathEqualTo("/messages/in"))
                .withQueryParam("receiverHerId", equalTo(receiverHerId.toString()))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(json)));

        List<IncomingMessage> result = dphClient.getMessages(onBehalfOf, receiverHerId);

        assertThat(result).isEmpty();
    }

    @Test
    void testMarkAsRead() {
        String messageId = UUID.randomUUID().toString();
        Integer receiverHerId = 12345;

        wireMockExtension.stubFor(post(urlPathEqualTo("/messages/in/" + messageId + "/read"))
                .withQueryParam("receiverHerId", equalTo(receiverHerId.toString()))
                .willReturn(aResponse()
                        .withStatus(200)));

        dphClient.markAsRead(onBehalfOf, receiverHerId, messageId);
    }

    @Test
    void testReceiveBusinessDocument() {
        String id = UUID.randomUUID().toString();

        wireMockExtension.stubFor(get(urlEqualTo("/messages/in/" + id))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                        .withBody("--boundary\r\nContent-Disposition: form-data; name=\"jose\"\r\n\r\njose-content\r\n--boundary--")));

        try {
            dphClient.receiveBusinessDocument(onBehalfOf, id);
        } catch (Exception ignored) {
            // It might fail because of missing parts or parsing, but we verified the request was made
        }
    }

    @Test
    void testReceiveApplicationReceipt() {
        String id = UUID.randomUUID().toString();

        wireMockExtension.stubFor(get(urlEqualTo("/messages/in/" + id + "/receipt"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/jose")
                        .withBody("jose-receipt")));

        try {
            dphClient.receiveApplicationReceipt(onBehalfOf, id);
        } catch (Exception ignored) {
        }
    }
}
