package no.difi.meldingsutveksling.cucumber;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.RequiredArgsConstructor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@RequiredArgsConstructor
public class WireMockMonitor {

    private final WireMockServer server;

    @PostConstruct
    public void start() {
        server.start();
    }

    @PreDestroy
    public void stop() {
        server.shutdown();
    }
}
