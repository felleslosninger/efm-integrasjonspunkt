package no.difi.meldingsutveksling.noarkexchange;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.KeystoreProvider;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerException;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.nextmove.AsicHandler;
import no.difi.meldingsutveksling.noarkexchange.altinn.MessagePolling;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.receipt.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecordWrapper;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import no.difi.vefa.peppol.lookup.LookupClient;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jms.core.JmsTemplate;

import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * CorrespondenceAgencyConfiguration class used for integration tests.
 * <p>
 * Contains mock overrides for all integration tests running with the profile "test". The test profile also has property files
 * located in src/test/resources/properties.
 */
@Profile("test")
@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class IntegrasjonspunktIntegrationTestConfig {

    @Bean
    public LookupClient getElmaLookupClient() {
        return mock(LookupClient.class);
    }

    @Autowired
    private IntegrasjonspunktProperties properties;

    @Bean
    public MessageSender messageSender(TransportFactory transportFactory,
                                       StandardBusinessDocumentFactory standardBusinessDocumentFactory,
                                       AsicHandler asicHandler,
                                       MessageContextFactory messageContextFactory) {
        return new MessageSender(transportFactory, standardBusinessDocumentFactory, asicHandler, messageContextFactory);
    }

    @Bean
    public MessageContextFactory messageContextFactory(Adresseregister adresseregister,
                                                       ServiceRegistryLookup serviceRegistryLookup) {
        return new MessageContextFactory(properties, adresseregister, serviceRegistryLookup);
    }

    @Bean
    @Primary
    public KeystoreProvider meldingsformidlerKeystoreProvider() throws MeldingsformidlerException {
        return mock(KeystoreProvider.class);
    }

    @Bean
    public SvarUtService svarUtService() {
        return mock(SvarUtService.class);
    }

    @Bean
    public StrategyFactory messageStrategyFactory(MessageSender messageSender,
                                                  ServiceRegistryLookup serviceRegistryLookup,
                                                  KeystoreProvider keystoreProvider,
                                                  SvarUtService svarUtService,
                                                  InternalQueue internalQueue) {
        return new StrategyFactory(messageSender, serviceRegistryLookup, keystoreProvider, properties, mock(NoarkClient.class), internalQueue);
    }

    @Bean
    @Primary
    public TransportFactory transportFactory() {
        TransportFactory transportFactoryMock = mock(TransportFactory.class);
        Transport transportMock = mock(Transport.class);
        doNothing().when(transportMock).send(any(ApplicationContext.class), any(StandardBusinessDocument.class));
        when(transportFactoryMock.createTransport(any(StandardBusinessDocument.class))).thenReturn(transportMock);
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
        when(sbdfMock.create(any(EDUCore.class), anyString(), any(Avsender.class), any(Mottaker.class))).thenReturn(mock(StandardBusinessDocument.class));
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
        ServiceRecordWrapper recordWrapper = ServiceRecordWrapper.of(sr, Lists.newArrayList(), Maps.newHashMap());
        when(srMock.getServiceRecord(anyString())).thenReturn(recordWrapper);
        when(srMock.getServiceRecord(anyString(), any(ServiceIdentifier.class))).thenReturn(Optional.ofNullable(sr));
        when(srMock.getServiceRecords(anyString())).thenReturn(Lists.newArrayList(sr));

        return srMock;
    }

    @Bean
    @Primary
    public RestClient restClient() {
        return mock(RestClient.class);
    }

    @Bean
    @Primary
    public ConversationService conversationService() {
        ConversationService conversationService = mock(ConversationService.class);
        when(conversationService.registerStatus(anyString(), any(MessageStatus.class))).thenReturn(Optional.empty());
        return conversationService;
    }

    @Bean
    @Primary
    public ConversationRepository conversationRepository() {
        return mock(ConversationRepository.class);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CmsUtil cmsUtil() {
        return new CmsUtil();
    }

    @Bean
    @Primary
    public Clock clock() {
        return Clock.fixed(Instant.parse("2019-03-25T11:38:23Z"), ZoneId.of("Europe/Oslo"));
    }

    @Bean
    public NoarkClient noarkClient() {
        return mock(NoarkClient.class);
    }

    @Bean
    public NoarkClient mailClient() {
        return mock(NoarkClient.class);
    }
}
