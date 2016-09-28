package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRequiredPropertyException;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.noarkexchange.altinn.MessagePolling;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.OrganizationType;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;

import javax.sql.DataSource;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CorrespondenceAgencyConfiguration class used for integration tests.
 *
 * Contains mock overrides for all integration tests running with the profile "test". The test profile
 * also has property files located in src/test/resources/properties.
 */
@Profile("test")
@Configuration
public class IntegrasjonspunktIntegrationTestConfig {

    @Autowired
    private Environment environment;

    @Bean
    public IntegrasjonspunktConfiguration integrasjonspunktConfiguration() throws MeldingsUtvekslingRequiredPropertyException {
        return new IntegrasjonspunktConfiguration(environment);
    }

    @Bean
    public MessageSender messageSender(TransportFactory transportFactory, Adresseregister adresseregister, IntegrasjonspunktConfiguration integrasjonspunktConfiguration, IntegrasjonspunktNokkel integrasjonspunktNokkel, StandardBusinessDocumentFactory standardBusinessDocumentFactory) {
        return new MessageSender(transportFactory, adresseregister, integrasjonspunktConfiguration, integrasjonspunktNokkel, standardBusinessDocumentFactory);
    }

    @Bean
    public StrategyFactory messageStrategyFactory(MessageSender messageSender, ServiceRegistryLookup serviceRegistryLookup) {
        return new StrategyFactory(messageSender, serviceRegistryLookup);
    }

    // Mocks

    @Bean
    @Primary
    public TransportFactory transportFactory() {
        TransportFactory transportFactoryMock = mock(TransportFactory.class);
        Transport transportMock = mock(Transport.class);
        doNothing().when(transportMock).send(any(Environment.class), any(EduDocument.class));
        when(transportFactoryMock.createTransport(any(EduDocument.class))).thenReturn(transportMock);
        return transportFactoryMock;
    }

    @Bean
    @Primary
    public MessagePolling messagePolling() {
        return mock(MessagePolling.class);
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
        return mock(Adresseregister.class);
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
    public DataSource dataSource() {
        DataSource dataSourceMock = mock(DataSource.class);
        return dataSourceMock;
    }

    @Bean
    @Primary
    public ServiceRegistryLookup serviceRegistryLookup(IntegrasjonspunktConfiguration integrasjonspunktConfiguration) throws URISyntaxException {
        ServiceRegistryLookup srMock = mock(ServiceRegistryLookup.class);
        InfoRecord ir = mock(InfoRecord.class);
        when(ir.getOrganisationNumber()).thenReturn("1337");
        when(ir.getOrganizationName()).thenReturn("foo");
        when(ir.getOrganizationType()).thenReturn(new OrganizationType("EDU", "EDU"));
        when(srMock.getInfoRecord(anyString())).thenReturn(ir);
        return srMock;
    }
}
