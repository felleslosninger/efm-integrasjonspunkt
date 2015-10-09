package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.noark.NoarkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoarkBeanFactory {
    @Autowired
    IntegrasjonspunktConfig integrasjonspunktConfig;

    @Bean(name="localNoark")
    public NoarkClient localNoark() {
        NoarkClient noarkClient = new NoarkClient();
        noarkClient.setSettings(integrasjonspunktConfig.getLocalNoarkClientSettings());
        return noarkClient;
    }

    @Bean(name="mshClient")
    public NoarkClient mshClient() {
        NoarkClient noarkClient = new NoarkClient();
        noarkClient.setSettings(integrasjonspunktConfig.getMshNoarkClientSettings());
        return noarkClient;
    }

}
