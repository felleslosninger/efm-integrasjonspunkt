package no.difi.meldingsutveksling.altinnv3.proxy;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class Tests {

    @MockitoBean
    private WebClient webclient;

    AltinnFunctions altinnFunctions = new AltinnFunctions(webclient);

    @Test
    public void setDigdirTokenInHeadersShouldSetTokenInHeaders() {
        String newToken = "token used in the request";

        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        var modifiedRequest = altinnFunctions.setDigdirTokenInHeaders(exchange, chain, newToken).block();

        var authHeader = modifiedRequest
            .getRequest()
            .getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);

        assertNotNull(authHeader, "Should have auth header");
        assertEquals("Bearer " + newToken, authHeader, "Should replace token in request with the newToken");
    }

    @Test
    public void isOrgOnAccessList() {
        var token = """
               {
                      "consumer" : {
                        "authority" : "iso6523-actorid-upis",
                        "ID" : "0192:311780735"
                      }
                    }
            """;
        var encoded = "header." + Base64.getEncoder().encodeToString(token.getBytes()) + ".signature";

        MockServerHttpRequest request = MockServerHttpRequest
            .get("/test")
            .header("Authorization", "Bearer " + encoded)
            .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        altinnFunctions.isOrgOnAccessList(exchange, List.of("311780735"));
    }
}
