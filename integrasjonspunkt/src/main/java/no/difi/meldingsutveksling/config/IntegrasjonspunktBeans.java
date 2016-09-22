package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.ServiceRegistryTransportFactory;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRequiredPropertyException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentFactory;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.net.URISyntaxException;

@Profile({"dev", "itest", "systest", "staging", "production"})
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

    @Bean
    public TransportFactory serviceRegistryTransportFactory(ServiceRegistryLookup serviceRegistryLookup, IntegrasjonspunktConfiguration integrasjonspunktConfiguration) {
        return new ServiceRegistryTransportFactory(serviceRegistryLookup);
    }

    @Bean
    public MessageSender messageSender(TransportFactory transportFactory, Adresseregister adresseregister, IntegrasjonspunktConfiguration integrasjonspunktConfiguration, IntegrasjonspunktNokkel integrasjonspunktNokkel, StandardBusinessDocumentFactory standardBusinessDocumentFactory) {
        return new MessageSender(transportFactory, adresseregister, integrasjonspunktConfiguration, integrasjonspunktNokkel, standardBusinessDocumentFactory);
    }

    @Bean
    public StrategyFactory messageStrategyFactory(MessageSender messageSender, ServiceRegistryLookup serviceRegistryLookup) {
        return new StrategyFactory(messageSender);
    }
}
