package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.ForsendelseMapper;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@ConditionalOnProperty(name="difi.move.fiks.enabled", havingValue = "true")
@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class SvarUtConfiguration {

    @Bean
    public ForsendelseMapper forsendelseMapper(IntegrasjonspunktProperties properties, ServiceRegistryLookup serviceRegistryLookup) {
        return new ForsendelseMapper(properties, serviceRegistryLookup);
    }

    @Bean
    public SvarUtService svarUtService(ForsendelseMapper forsendelseMapper, SvarUtWebServiceClient svarUtClient, ServiceRegistryLookup serviceRegistryLookup) {
        return new SvarUtService(svarUtClient, serviceRegistryLookup, forsendelseMapper);
    }

}