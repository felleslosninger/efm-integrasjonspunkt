package no.difi.meldingsutveksling.noarkexchange;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noark.NoarkClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class NoarkBeanFactory {

    private final IntegrasjonspunktProperties properties;

    @Bean(name = "localNoark")
    public NoarkClient localNoark() {
        NoarkClientSettings clientSettings = new NoarkClientSettings(
                properties.getNoarkSystem().getEndpointURL(),
                properties.getNoarkSystem().getUsername(),
                properties.getNoarkSystem().getPassword(),
                properties.getNoarkSystem().getDomain());
        return new NoarkClientFactory(clientSettings).from(properties);
    }

    @Bean(name = "mshClient")
    public NoarkClient mshClient() {
        return new MshClient(properties.getMsh().getEndpointURL());
    }
}
