package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.ServiceRegistryTransportFactory;
import no.difi.meldingsutveksling.auth.OidcTokenClient;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentFactory;
import no.difi.meldingsutveksling.noarkexchange.putmessage.KeystoreProvider;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.receipt.DpiReceiptService;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({"dev", "itest", "systest", "staging", "production"})
@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class IntegrasjonspunktBeans {

    @Autowired
    private IntegrasjonspunktProperties properties;

    @Autowired
    private OidcTokenClient oidcClient;

    @Bean
    public AltinnFormidlingsTjenestenConfig altinnConfig() {
        return properties.getAltinn();
    }

    @Bean
    public TransportFactory serviceRegistryTransportFactory(ServiceRegistryLookup serviceRegistryLookup) {
        return new ServiceRegistryTransportFactory(serviceRegistryLookup);
    }

    @Bean
    public IntegrasjonspunktNokkel integrasjonspunktNokkel() {
        return new IntegrasjonspunktNokkel(properties.getOrg().getKeystore());
    }

    @Bean
    public MessageSender messageSender(TransportFactory transportFactory, Adresseregister adresseregister, IntegrasjonspunktNokkel integrasjonspunktNokkel, StandardBusinessDocumentFactory standardBusinessDocumentFactory) {
        return new MessageSender(transportFactory, adresseregister, properties, integrasjonspunktNokkel, standardBusinessDocumentFactory);
    }

    @Bean
    public KeystoreProvider meldingsformidlerKeystoreProvider() throws MeldingsformidlerException {
        return KeystoreProvider.from(properties);
    }

    @Bean
    public StrategyFactory messageStrategyFactory(MessageSender messageSender, ServiceRegistryLookup serviceRegistryLookup, KeystoreProvider meldingsformidlerKeystoreProvider) {
        return new StrategyFactory(messageSender, serviceRegistryLookup, meldingsformidlerKeystoreProvider);
    }

    @Bean
    public DpiReceiptService dpiReceiptService(IntegrasjonspunktProperties integrasjonspunktProperties, KeystoreProvider keystoreProvider) {
        return new DpiReceiptService(integrasjonspunktProperties, keystoreProvider);
    }

}
