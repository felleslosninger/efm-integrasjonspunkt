package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.noark.NoarkClientFactory;
import no.difi.meldingsutveksling.noark.NoarkClientOld;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoarkBeanFactory {
    @Autowired
    IntegrasjonspunktConfig integrasjonspunktConfig;

    @Bean(name="localNoark")
    public NoarkClient localNoark() {
        NoarkClientOld noarkClient = new NoarkClientOld();
        noarkClient.setSettings(integrasjonspunktConfig.getLocalNoarkClientSettings());

        return new NoarkClientFactory().from(integrasjonspunktConfig);
    }

    @Bean(name="mshClient")
    public NoarkClient mshClient() {
        NoarkClientOld noarkClient = new NoarkClientOld();
        noarkClient.setSettings(integrasjonspunktConfig.getMshNoarkClientSettings());
        return new NoarkClientFactory().from(integrasjonspunktConfig);
    }

}
