package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.ks.SvarUtService;
import no.difi.meldingsutveksling.noarkexchange.altinn.MessagePolling;
import no.difi.meldingsutveksling.noarkexchange.putmessage.KeystoreProvider;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.receipt.StatusStrategyFactory;
import no.difi.meldingsutveksling.receipt.DpiReceiptService;
import no.difi.meldingsutveksling.receipt.ReceiptPolling;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import no.difi.move.common.oauth.KeystoreHelper;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;

import java.net.URISyntaxException;

import static org.mockito.Mockito.*;

/**
 * CorrespondenceAgencyConfiguration class used for integration tests.
 *
 * Contains mock overrides for all integration tests running with the profile "test". The test profile also has property files
 * located in src/test/resources/properties.
 */
@Profile("test")
@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class IntegrasjonspunktIntegrationTestConfig {

    @Autowired
    private IntegrasjonspunktProperties properties;

    @Bean
    public MessageSender messageSender(TransportFactory transportFactory, Adresseregister adresseregister,
                                       IntegrasjonspunktNokkel integrasjonspunktNokkel,
                                       StandardBusinessDocumentFactory standardBusinessDocumentFactory,
                                       ServiceRegistryLookup serviceRegistryLookup) {
        return new MessageSender(transportFactory, adresseregister, properties, integrasjonspunktNokkel,
                standardBusinessDocumentFactory, serviceRegistryLookup);
    }

    @Bean
    @Primary
    public KeystoreProvider meldingsformidlerKeystoreProvider() throws MeldingsformidlerException {
        return mock(KeystoreProvider.class);
    }

    @Bean(name = "signingKeystoreHelper")
    @Primary
    public KeystoreHelper keystoreHelper() {
        return mock(KeystoreHelper.class);
    }

    @Bean
    public SvarUtService svarUtService() {
        return mock(SvarUtService.class);
    }

    @Bean
    public StrategyFactory messageStrategyFactory(MessageSender messageSender, ServiceRegistryLookup serviceRegistryLookup, KeystoreProvider keystoreProvider, SvarUtService svarUtService) {
        return new StrategyFactory(messageSender, serviceRegistryLookup, keystoreProvider, properties);
    }

    // Mocks
    @Bean
    @Primary
    public TransportFactory transportFactory() {
        TransportFactory transportFactoryMock = mock(TransportFactory.class);
        Transport transportMock = mock(Transport.class);
        doNothing().when(transportMock).send(any(ApplicationContext.class), any(EduDocument.class));
        when(transportFactoryMock.createTransport(any(EduDocument.class))).thenReturn(transportMock);
        return transportFactoryMock;
    }

    @Bean
    public DpiReceiptService dpiReceiptService(IntegrasjonspunktProperties properties, KeystoreProvider keystoreProvider) {
        return new DpiReceiptService(properties, keystoreProvider);
    }

    @Bean
    @Primary
    public MessagePolling messagePolling() {
        return mock(MessagePolling.class);
    }

    @Bean
    @Primary
    public ReceiptPolling receiptPolling() {
        return mock(ReceiptPolling.class);
    }

    @Bean
    @Primary
    public StatusStrategyFactory statusStrategyFactory() {
        return mock(StatusStrategyFactory.class);
    }

    @Bean
    @Primary
    public JmsTemplate jmsTemplate() {
        return mock(JmsTemplate.class);
    }

    @Bean
    @Primary
    public ActiveMQConnectionFactory activeMQConnectionFactory() {
        return mock(ActiveMQConnectionFactory.class);
    }

    @Bean
    @Primary
    public Adresseregister adresseregister() {
        Adresseregister adresseregisterMock = mock(Adresseregister.class);
        when(adresseregisterMock.hasAdresseregisterCertificate(any(ServiceRecord.class))).thenReturn(true);
        return adresseregisterMock;
    }

    @Bean
    @Primary
    public IntegrasjonspunktNokkel integrasjonspunktNokkel() {
        return mock(IntegrasjonspunktNokkel.class);
    }

    @Bean
    @Primary
    public StandardBusinessDocumentFactory standardBusinessDocumentFactory() throws MessageException {
        StandardBusinessDocumentFactory sbdfMock = mock(StandardBusinessDocumentFactory.class);
        when(sbdfMock.create(any(EDUCore.class), anyString(), any(Avsender.class), any(Mottaker.class))).thenReturn(mock(EduDocument.class));
        return sbdfMock;
    }

    @Bean
    @Primary
    public ServiceRegistryLookup serviceRegistryLookup() throws URISyntaxException {
        ServiceRegistryLookup srMock = mock(ServiceRegistryLookup.class);
        InfoRecord ir = mock(InfoRecord.class);
        when(ir.getIdentifier()).thenReturn("1337");
        when(ir.getOrganizationName()).thenReturn("foo");
        when(ir.getEntityType()).thenReturn(new EntityType("Organisasjonsledd", "ORGL"));
        when(srMock.getInfoRecord(anyString())).thenReturn(ir);

        ServiceRecord sr = mock(ServiceRecord.class);
        when(sr.getServiceIdentifier()).thenReturn(ServiceIdentifier.DPO);
        when(sr.getOrganisationNumber()).thenReturn("1337");
        when(srMock.getServiceRecord(anyString())).thenReturn(sr);

        return srMock;
    }
}
