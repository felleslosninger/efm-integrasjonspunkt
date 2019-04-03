package no.difi.meldingsutveksling.cucumber;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceServer;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("cucumber")
public class MockWebServiceServerCustomizer {

    private final Map<WebServiceTemplate, MockWebServiceServer> servers = new ConcurrentHashMap<>();

    void customize(WebServiceTemplate webServiceTemplate) {
        MockWebServiceServer server = MockWebServiceServer.createServer(webServiceTemplate);
        this.servers.put(webServiceTemplate, server);
    }

    MockWebServiceServer getServer(WebServiceTemplate webServiceTemplate) {
        return this.servers.get(webServiceTemplate);
    }

    Map<WebServiceTemplate, MockWebServiceServer> getServers() {
        return Collections.unmodifiableMap(this.servers);
    }
}
