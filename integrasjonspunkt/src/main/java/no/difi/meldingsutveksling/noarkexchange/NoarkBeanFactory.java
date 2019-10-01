package no.difi.meldingsutveksling.noarkexchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noark.NoarkClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class NoarkBeanFactory {

    private final IntegrasjonspunktProperties properties;

    @Bean(name = "localNoark")
    @ConditionalOnProperty(value = "difi.move.noarkSystem.type")
    public NoarkClient localNoark() {
        NoarkClientSettings clientSettings = new NoarkClientSettings(
                properties.getNoarkSystem().getEndpointURL(),
                properties.getNoarkSystem().getUsername(),
                properties.getNoarkSystem().getPassword(),
                properties.getNoarkSystem().getDomain());
        NoarkClient noarkClient = new NoarkClientFactory(clientSettings).from(properties);
        log.info("NoarkClient created of type: {}", noarkClient.getClass().getSimpleName());
        return noarkClient;
    }

}
