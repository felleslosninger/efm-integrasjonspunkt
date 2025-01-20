package no.difi.meldingsutveksling.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.webhooks.UrlPusher;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebHookConfig {

    private final IntegrasjonspunktProperties integrasjonspunktProperties;

    @Bean
    public UrlPusher urlPusher(RestTemplateBuilder restTemplateBuilder) {

        IntegrasjonspunktProperties.WebHooks webHooks = integrasjonspunktProperties.getWebhooks();

        return new UrlPusher(restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(webHooks.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(webHooks.getReadTimeout()))
                .errorHandler(new DefaultResponseErrorHandler() {
                    @Override
                    protected void handleError(ClientHttpResponse response, HttpStatusCode statusCode) throws IOException {
                        log.info("Webhook push failed with: {} {}", statusCode, response.getStatusText());
                    }
                })
                .build());
    }

}
