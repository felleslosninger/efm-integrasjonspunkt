package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.noark.NoarkClientOld;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoarkBeanFactory {
    @Autowired
    IntegrasjonspunktConfig integrasjonspunktConfig;

    @Bean(name="localNoark")
    public NoarkClientOld localNoark() {
        NoarkClientOld noarkClient = new NoarkClientOld();
        noarkClient.setSettings(integrasjonspunktConfig.getLocalNoarkClientSettings());
        return noarkClient;
    }

    @Bean(name="mshClient")
    public NoarkClientOld mshClient() {
        NoarkClientOld noarkClient = new NoarkClientOld();
        noarkClient.setSettings(integrasjonspunktConfig.getMshNoarkClientSettings());
        return noarkClient;
    }

}
