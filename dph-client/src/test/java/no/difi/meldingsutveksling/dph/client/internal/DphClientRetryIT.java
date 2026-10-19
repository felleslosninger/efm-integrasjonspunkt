package no.difi.meldingsutveksling.dph.client.internal;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import no.difi.meldingsutveksling.domain.Iso6523;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@MockitoSettings(strictness = Strictness.LENIENT)
class DphClientRetryIT {

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

        given(createMaskinportenToken.createMaskinportenToken(onBehalfOf)).willReturn("dummy-token");
    }

    @Test
    void shouldNotRetryOn4xx() {
        String messageId = "test-id";
        wireMockExtension.stubFor(get(urlEqualTo("/messages/out/" + messageId + "/statuses"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"errorCode\": \"400\", \"message\": \"Bad Request\"}")));

        assertThatExceptionOfType(Exception.class)
            .isThrownBy(() -> dphClient.getStatus(onBehalfOf, messageId));

        wireMockExtension.verify(1, getRequestedFor(urlEqualTo("/messages/out/" + messageId + "/statuses")));
    }

    @Test
    void shouldRetryOn5xx() {
        String messageId = "test-id";
        wireMockExtension.stubFor(get(urlEqualTo("/messages/out/" + messageId + "/statuses"))
            .willReturn(aResponse()
                .withStatus(500)));

        assertThatExceptionOfType(Exception.class)
            .isThrownBy(() -> dphClient.getStatus(onBehalfOf, messageId));

        // 1 original call + 3 retries = 4 total calls
        wireMockExtension.verify(4, getRequestedFor(urlEqualTo("/messages/out/" + messageId + "/statuses")));
    }
}
