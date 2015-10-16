package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.noark.NoarkClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoarkBeanFactory {
    @Autowired
    IntegrasjonspunktConfig integrasjonspunktConfig;

    @Bean(name="localNoark")
    public NoarkClient localNoark() {
        NoarkClient client = new NoarkClientFactory().from(integrasjonspunktConfig);
        client.setSettings(integrasjonspunktConfig.getLocalNoarkClientSettings());
        return client;
    }

    @Bean(name="mshClient")
    public NoarkClient mshClient() {
        NoarkClient client = new NoarkClientFactory().from(integrasjonspunktConfig);
        client.setSettings(integrasjonspunktConfig.getMshNoarkClientSettings());
        return client;
    }

}
