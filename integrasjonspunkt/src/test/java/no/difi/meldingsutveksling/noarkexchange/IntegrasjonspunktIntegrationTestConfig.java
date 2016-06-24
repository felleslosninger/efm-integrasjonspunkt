package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.noarkexchange.altinn.MessagePolling;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;

import javax.sql.DataSource;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Configuration class used for integration tests.
 *
 * Contains mock overrides for all integration tests running with the profile "test". The test profile
 * also has property files located in src/test/resources/properties.
 */
@Profile("test")
@Configuration
public class IntegrasjonspunktIntegrationTestConfig {

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
        when(sbdfMock.create(any(PutMessageRequestType.class), anyString(), any(Avsender.class), any(Mottaker.class))).thenReturn(mock(EduDocument.class));
        return sbdfMock;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        DataSource dataSourceMock = mock(DataSource.class);
        return dataSourceMock;
    }

}
