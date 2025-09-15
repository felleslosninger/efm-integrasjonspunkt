package no.difi.meldingsutveksling.altinnv3.token;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenServiceTest {

    private WireMockServer wireMockServer;
    private TokenService tokenService;
    private TokenExchangeService tokenExchangeService;

    private static String path = "read-from-application-properties";
    private static String alias = "read-from-application-properties";
    private static String password = "read-from-application-properties";

    @BeforeAll
    static void init() throws Exception {
        try (InputStream inputStream = TokenServiceTest.class.getClassLoader().getResourceAsStream("application.properties")) {
            var props = new Properties();
            props.load(inputStream);
            path = (String) props.get("difi.move.org.keystore.path");
            alias = (String) props.get("difi.move.org.keystore.alias");
            password = (String) props.get("difi.move.org.keystore.password");
        }
    }

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        //WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testFetchToken() throws JsonProcessingException {

        // mock the maskinporten token endpoint
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/token"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "access_token": "test-access-token",
                                    "token_type": "Bearer",
                                    "expires_in": 119,
                                    "scope":"altinn:broker.write altinn:broker.read altinn:serviceowner"
                                }
                                """)));

        // mock the token exchange endpoint
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/exchange"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody("exchanged-token")));

        // manually create services since we're not in a spring context
        tokenService = new MaskinportenTokenService();
        tokenExchangeService = new AltinnTokenExchangeService(RestClient.create());

        // we need to customize the deserialization of Resource to handle file paths
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new MyModule());

        var tc = objectMapper.readValue("""
            {
                "oidc": {
                    "url": "%s/token",
                    "audience": "https://test.maskinporten.no/",
                    "clientId": "a63cac91-3210-4c35-b961-5c7bf122345c",
                    "keystore": {
                        "alias": "%s",
                        "password": "%s",
                        "type": "jks",
                        "path": "%s"
                    }
                },
                "exchangeUrl": "%s/exchange"
            }
            """.formatted(wireMockServer.baseUrl(), alias, password, path, wireMockServer.baseUrl()), TokenConfig.class);

        var token = tokenService.fetchToken(tc, List.of("altinn:broker.read","altinn:broker.write","altinn:serviceowner"));
        assertEquals("test-access-token", token, "Token should match the mocked access token");

        var exchangedToken = tokenExchangeService.exchangeToken(token, tc.exchangeUrl());
        assertEquals("exchanged-token", exchangedToken, "Token should match the mocked access token");

    }

    public class ResourceDeserializer extends JsonDeserializer<Resource> {
        @Override
        public Resource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String path = p.getText().substring(5);
            return new FileSystemResource(path);
        }
    }

    public class MyModule extends SimpleModule {
        public MyModule() {
            super("MyModule");
            addDeserializer(Resource.class, new ResourceDeserializer());
        }
    }

}
