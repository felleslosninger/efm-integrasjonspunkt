package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRequiredPropertyException;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.URISyntaxException;

@Configuration
public class IntegrasjonspunktBeans {
    @Autowired
    private Environment environment;

    @Bean
    public IntegrasjonspunktConfiguration integrasjonspunktConfiguration() throws MeldingsUtvekslingRequiredPropertyException {
        return new IntegrasjonspunktConfiguration(environment);
    }

    @Bean
    public ServiceRegistryLookup serviceRegistryLookup(IntegrasjonspunktConfiguration integrasjonspunktConfiguration) throws URISyntaxException {
        return new ServiceRegistryLookup(new RestClient(integrasjonspunktConfiguration.getServiceRegistryUrl()));
    }
}
