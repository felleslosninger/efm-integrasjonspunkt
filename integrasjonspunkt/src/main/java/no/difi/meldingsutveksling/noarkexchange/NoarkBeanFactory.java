package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.noark.NoarkClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class NoarkBeanFactory {

    @Autowired
    IntegrasjonspunktConfig integrasjonspunktConfig;

    @Bean(name="localNoark")
    public NoarkClient localNoark() {
        NoarkClientSettings clientSettings = integrasjonspunktConfig.getLocalNoarkClientSettings();
        NoarkClient client = new NoarkClientFactory(clientSettings).from(integrasjonspunktConfig);
        return client;
    }

    @Bean(name="mshClient")
    public NoarkClient mshClient() {
        NoarkClientSettings clientSettings = integrasjonspunktConfig.getMshNoarkClientSettings();
        NoarkClient client = new NoarkClientFactory(clientSettings).from(integrasjonspunktConfig);
        return client;
    }
}