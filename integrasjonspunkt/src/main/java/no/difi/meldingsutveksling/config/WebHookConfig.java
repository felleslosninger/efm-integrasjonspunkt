package no.difi.meldingsutveksling.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.webhooks.UrlPusher;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class WebHookConfig {

    private final IntegrasjonspunktProperties integrasjonspunktProperties;

    @Bean
    public UrlPusher urlPusher(RestTemplateBuilder restTemplateBuilder) {

        IntegrasjonspunktProperties.WebHooks webHooks = integrasjonspunktProperties.getWebhooks();

        return new UrlPusher(restTemplateBuilder
                .setConnectTimeout(webHooks.getConnectTimeout())
                .setReadTimeout(webHooks.getReadTimeout())
                .build());
    }
}
