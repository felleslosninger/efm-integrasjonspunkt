package no.difi.meldingsutveksling.serviceregistry.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.nimbusds.jose.proc.BadJWSException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.oauth.JWTDecoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static no.difi.meldingsutveksling.serviceregistry.client.ServiceRegistryRestClient.X_ENABLE_BETA_FEATURES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;

@ExtendWith(MockitoExtension.class)
class ServiceRegistryRestClientTest {

    private static final String BODY = "{ \"content\": \"1 2 3\" }";
    private static final String SIGNED_BODY = "{ \"signed\": \"1 2 3\" }";

    private ServiceRegistryRestClient target;

    @Mock
    private IntegrasjonspunktProperties properties;

    @Mock
    private IntegrasjonspunktProperties.Sign sign;

    @Mock
    private IntegrasjonspunktProperties.FeatureToggle featureToggle;

    @Mock
    private JWTDecoder jwtDecoder;

    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension.newInstance()
        .options(wireMockConfig()
            .port(9800))
        .build();

    @BeforeEach
    void beforeEach() {
        when(properties.getSign()).thenReturn(sign);
        when(properties.getFeature()).thenReturn(featureToggle);
        target = new ServiceRegistryRestClient(properties, RestClient.builder().build(), jwtDecoder, URI.create("http://localhost:9800"));
    }

    @AfterEach
    void afterEach() {
        wireMockExtension.resetAll();
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
        false, false
        true, false
        false, true
        true, true
        """)
    void getResource(boolean enableBetaFeaturesHeader, boolean signedContent) throws BadJWSException, MalformedURLException {
        when(sign.isEnable()).thenReturn(signedContent);
        when(featureToggle.isEnableBetaFeatures()).thenReturn(enableBetaFeaturesHeader);

        if (signedContent) {
            when(sign.getJwkUrl()).thenReturn(URI.create("http://localhost:8080/jwk").toURL());
            when(jwtDecoder.getPayload(any(), any(URL.class))).thenReturn(BODY);
        }

        wireMockExtension.givenThat(WireMock.get(WireMock.urlEqualTo("/identifier/09118532322"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(signedContent ? SIGNED_BODY : BODY)
            )
        );

        String resource = target.getResource("/identifier/09118532322");

        assertThat(resource).isEqualTo(BODY);

        RequestPatternBuilder requestedFor = WireMock.getRequestedFor(WireMock.urlEqualTo("/identifier/09118532322"));

        if (enableBetaFeaturesHeader) {
            requestedFor = requestedFor.withHeader(X_ENABLE_BETA_FEATURES, WireMock.equalTo("true"));
        } else {
            requestedFor = requestedFor.withoutHeader(X_ENABLE_BETA_FEATURES);
        }

        if (signedContent) {
            requestedFor = requestedFor.withHeader(ACCEPT, WireMock.havingExactly("application/jose", "application/json"));
            verify(jwtDecoder).getPayload(SIGNED_BODY, URI.create("http://localhost:8080/jwk").toURL());
        }

        wireMockExtension.verify(requestedFor);
    }
}
