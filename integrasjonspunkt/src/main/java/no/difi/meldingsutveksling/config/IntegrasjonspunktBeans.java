package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.ServiceRegistryTransportFactory;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.ks.SvarUtService;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentFactory;
import no.difi.meldingsutveksling.noarkexchange.putmessage.FiksMessageStrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.putmessage.KeystoreProvider;
import no.difi.meldingsutveksling.noarkexchange.putmessage.MessageStrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.ConversationStrategy;
import no.difi.meldingsutveksling.receipt.ConversationStrategyFactory;
import no.difi.meldingsutveksling.receipt.DpiReceiptService;
import no.difi.meldingsutveksling.receipt.strategy.FiksConversationStrategy;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.TransportFactory;
import no.difi.move.common.config.KeystoreProperties;
import no.difi.move.common.oauth.KeystoreHelper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Profile({"dev", "itest", "systest", "staging", "production"})
@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class IntegrasjonspunktBeans {

    private final IntegrasjonspunktProperties properties;

    @Autowired
    public IntegrasjonspunktBeans(IntegrasjonspunktProperties properties) {
        this.properties = properties;
    }

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
    public MessageSender messageSender(TransportFactory transportFactory, Adresseregister adresseregister,
                                       IntegrasjonspunktNokkel integrasjonspunktNokkel,
                                       StandardBusinessDocumentFactory standardBusinessDocumentFactory,
                                       ServiceRegistryLookup serviceRegistryLookup) {
        return new MessageSender(transportFactory, adresseregister, properties, integrasjonspunktNokkel,
                standardBusinessDocumentFactory, serviceRegistryLookup);
    }

    @Bean
    public KeystoreProvider meldingsformidlerKeystoreProvider() throws MeldingsformidlerException {
        return KeystoreProvider.from(properties);
    }

    @ConditionalOnProperty(name="difi.move.fiks.enabled", havingValue = "true")
    @Bean
    public FiksMessageStrategyFactory fiksMessageStrategyFactory(SvarUtService svarUtService) {
        return FiksMessageStrategyFactory.newInstance(svarUtService);
    }

    @ConditionalOnProperty(name="difi.move.fiks.enabled", havingValue = "true")
    @Bean
    public FiksConversationStrategy fiksConversationStrategy(SvarUtService svarUtService, ConversationRepository conversationRepository) {
        return new FiksConversationStrategy(svarUtService, conversationRepository);
    }

    @Bean
    public ConversationStrategyFactory conversationStrategyFactory(List<ConversationStrategy> conversationStrategies) {
        ConversationStrategyFactory conversationStrategyFactory = new ConversationStrategyFactory();
        conversationStrategies.forEach(conversationStrategyFactory::registerStrategy);
        return conversationStrategyFactory;
    }

    @Bean
    public StrategyFactory messageStrategyFactory(MessageSender messageSender, ServiceRegistryLookup serviceRegistryLookup, KeystoreProvider meldingsformidlerKeystoreProvider,
                                                  @SuppressWarnings("SpringJavaAutowiringInspection") ObjectProvider<List<MessageStrategyFactory>> messageStrategyFactory) {
        final StrategyFactory strategyFactory = new StrategyFactory(messageSender, serviceRegistryLookup, meldingsformidlerKeystoreProvider, properties);
        if(messageStrategyFactory.getIfAvailable() != null) {
            messageStrategyFactory.getIfAvailable().forEach(strategyFactory::registerMessageStrategyFactory);
        }
        return strategyFactory;
    }



    @Bean
    public DpiReceiptService dpiReceiptService(IntegrasjonspunktProperties integrasjonspunktProperties, KeystoreProvider keystoreProvider) {
        return new DpiReceiptService(integrasjonspunktProperties, keystoreProvider);
    }

    @Bean(name = "signingKeystoreHelper")
    public KeystoreHelper signingKeystoreHelper() {
        KeystoreProperties keystoreProperties = new KeystoreProperties();
        keystoreProperties.setLocation(properties.getSign().getKeystore().getPath());
        keystoreProperties.setAlias(properties.getSign().getKeystore().getAlias());
        keystoreProperties.setEntryPassword(properties.getSign().getKeystore().getPassword());
        keystoreProperties.setStorePassword(properties.getSign().getKeystore().getPassword());
        return new KeystoreHelper(keystoreProperties);
    }
}
