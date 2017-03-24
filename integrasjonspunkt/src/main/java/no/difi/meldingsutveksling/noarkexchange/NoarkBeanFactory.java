package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noark.NoarkClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class NoarkBeanFactory {

    @Autowired
    IntegrasjonspunktProperties properties;

    @Bean(name = "localNoark")
    public NoarkClient localNoark() {
        NoarkClientSettings clientSettings = new NoarkClientSettings(
                properties.getNoarkSystem().getEndpointURL(),
                properties.getNoarkSystem().getUsername(),
                properties.getNoarkSystem().getPassword(),
                properties.getNoarkSystem().getDomain());
        NoarkClient client = new NoarkClientFactory(clientSettings).from(properties);
        return client;
    }

    @Bean(name = "mshClient")
    public NoarkClient mshClient() {
        return new MshClient(properties.getMsh().getEndpointURL());
    }
}
