package no.difi.meldingsutveksling.cucumber;

import com.github.tomakehurst.wiremock.WireMockServer;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.IntegrasjonspunktApplication;
import no.difi.meldingsutveksling.KeystoreProvider;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.clock.TestClockConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.SikkerDigitalPostKlientFactory;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnConnectionCheck;
import no.difi.meldingsutveksling.ks.svarut.SvarUtConnectionCheck;
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceClientImpl;
import no.difi.meldingsutveksling.nextmove.ServiceBusRestTemplate;
import no.difi.meldingsutveksling.noark.NoarkClientFactory;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.NoarkClientSettings;
import no.difi.meldingsutveksling.noarkexchange.altinn.AltinnConnectionCheck;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.mapping.CorrespondenceAgencyConnectionCheck;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.webhooks.WebhookPusher;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.AktoerOrganisasjonsnummer;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.webservice.support.SoapFaultInterceptorLogger;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.axiom.AxiomSoapMessageFactory;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.http.AbstractHttpWebServiceMessageSender;

import javax.xml.bind.Marshaller;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ContextConfiguration(classes = {
        IntegrasjonspunktApplication.class,
        CucumberStepsConfiguration.SpringConfiguration.class,
        TestClockConfig.class
}, loader = SpringBootContextLoader.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("cucumber")
@AutoConfigureWebClient(registerRestTemplate = true)
@Slf4j
@TestPropertySource
public class CucumberStepsConfiguration {

    @Configuration
    @Profile("cucumber")
    @RequiredArgsConstructor
    @SpyBean(WebhookPusher.class)
    public static class SpringConfiguration {

        @Bean
        @Primary
        public TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }

        @Bean
        public SaajSoapMessageFactory saajSoapMessageFactory() {
            SaajSoapMessageFactory messageFactory = new SaajSoapMessageFactory();
            messageFactory.setSoapVersion(SoapVersion.SOAP_11);
            return messageFactory;
        }

        @Bean
        public WireMockServer wireMockServer() {
            return new WireMockServer(options().port(9800));
        }

        @Bean
        public WireMockMonitor wireMockMonitor(WireMockServer wireMockServer) {
            return new WireMockMonitor(wireMockServer);
        }

        @Primary
        @Bean
        public SikkerDigitalPostKlient sikkerDigitalPostKlient(IntegrasjonspunktProperties properties,
                                                               KeystoreProvider keystoreProvider,
                                                               RequestCaptureClientInterceptor requestCaptureClientInterceptor) {
            SikkerDigitalPostKlientFactory factory = new SikkerDigitalPostKlientFactory(properties.getDpi(), keystoreProvider.getKeyStore());
            SikkerDigitalPostKlient klient = factory.createSikkerDigitalPostKlient(
                    AktoerOrganisasjonsnummer.of("910077473"));
            klient.getMeldingTemplate().setInterceptors(new ClientInterceptor[]{
                    requestCaptureClientInterceptor, new FakeEbmsClientInterceptor()});

            Jaxb2Marshaller marshaller = (Jaxb2Marshaller) klient.getMeldingTemplate().getMarshaller();

            Map<String, Object> marshallerProperties = new HashMap<>();
            marshallerProperties.put(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshallerProperties.put(XMLMarshaller.PREFIX_MAPPER, new DefaultNamespacePrefixMapper());
            marshaller.setMarshallerProperties(marshallerProperties);

            return klient;
        }

        @Bean
        @Primary
        public IntegrasjonspunktProperties properties(IntegrasjonspunktProperties properties) {
            IntegrasjonspunktProperties spy = spy(properties);
            given(spy.getNoarkSystem()).willReturn(spy(properties.getNoarkSystem()));
            return spy;
        }

        @Bean
        public CachingWebServiceTemplateFactory cachingWebServiceTemplateFactory(
                RequestCaptureClientInterceptor requestCaptureClientInterceptor
        ) {
            return new CachingWebServiceTemplateFactory(requestCaptureClientInterceptor);
        }

        @Primary
        @Bean(name = "localNoark")
        public NoarkClient localNoark(CachingWebServiceTemplateFactory cachingWebServiceTemplateFactory,
                                      IntegrasjonspunktProperties properties) {
            NoarkClientSettings clientSettings = spy(new NoarkClientSettings(
                    properties.getNoarkSystem().getEndpointURL(),
                    properties.getNoarkSystem().getUsername(),
                    properties.getNoarkSystem().getPassword(),
                    properties.getNoarkSystem().getDomain()));

            given(clientSettings.createTemplateFactory()).willReturn(cachingWebServiceTemplateFactory);

            return new NoarkClientFactory(clientSettings).from(properties);
        }

        @Bean
        @Primary
        public CorrespondenceAgencyClient correspondenceAgencyClient(
                CorrespondenceAgencyConfiguration config,
                RequestCaptureClientInterceptor requestCaptureClientInterceptor) {
            return new CorrespondenceAgencyClient(config) {

                @Override
                protected List<ClientInterceptor> getAdditionalInterceptors() {
                    return Collections.singletonList(requestCaptureClientInterceptor);
                }

                protected Map<String, Object> getMarshallerProperties() {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                    properties.put(XMLMarshaller.PREFIX_MAPPER, new DefaultNamespacePrefixMapper());
                    return properties;
                }
            };
        }

        @Bean
        @Primary
        public SvarUtWebServiceClientImpl svarUtClient(RequestCaptureClientInterceptor requestCaptureClientInterceptor, Jaxb2Marshaller marshaller, AxiomSoapMessageFactory svarUtMessageFactory, AbstractHttpWebServiceMessageSender svarUtMessageSender) {
            SvarUtWebServiceClientImpl client = new SvarUtWebServiceClientImpl();
            client.setDefaultUri("http://localhost:8080");
            client.setMarshaller(marshaller);
            client.setUnmarshaller(marshaller);

            client.setMessageFactory(svarUtMessageFactory);
            client.setMessageSender(svarUtMessageSender);

            final ClientInterceptor[] interceptors = new ClientInterceptor[2];
            interceptors[0] = SoapFaultInterceptorLogger.withLogMarkers(Markers.append("serviceidentifier", "fiks"));
            interceptors[1] = requestCaptureClientInterceptor;
            client.setInterceptors(interceptors);

            return client;
        }

        /**
         * Hack to avoid problems when creating PostVirksomhetStrategyFactory
         */
        @Bean
        @Primary
        public ServiceRegistryLookup init(ServiceRegistryLookup serviceRegistryLookup) {
            ServiceRegistryLookup spy = spy(serviceRegistryLookup);
            doReturn(new InfoRecord()
                    .setOrganizationName("Test - C2")).when(spy).getInfoRecord("974720760");
            return spy;
        }

        @Bean
        @Primary
        public TaskScheduler poolScheduler() {
            return new NoopTaskScheduler();
        }

        @Bean
        public AsicParser asicParser() {
            return new AsicParser();
        }

        @Bean
        public CucumberKeyStore cucumberKeyStore(IntegrasjonspunktProperties properties) {
            return new CucumberKeyStore(properties.getOrg().getKeystore());
        }

        @Bean
        public Holder<ZipContent> zipContentHolder() {
            return new Holder<>();
        }

        @Bean
        public Holder<Message> messageInHolder() {
            return new Holder<>();
        }

        @Bean
        public Holder<Message> messageReceivedHolder() {
            return new Holder<>();
        }

        @Bean
        public Holder<Message> messageOutHolder() {
            return new Holder<>();
        }

        @Bean
        public Holder<Message> messageSentHolder() {
            return new Holder<>();
        }

        @Bean
        public Holder<List<String>> webServicePayloadHolder() {
            return new Holder<>();
        }
    }

    @Rule
    private final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @MockBean public UUIDGenerator uuidGenerator;
    @MockBean public LookupClient lookupClient;
    @MockBean public InternalQueue internalQueue;
    @MockBean public ServiceBusRestTemplate serviceBusRestTemplate;
    @MockBean public SikkerDigitalPostKlientFactory sikkerDigitalPostKlientFactory;
    @MockBean public SvarUtConnectionCheck svarUtConnectionCheck;
    @MockBean public SvarInnConnectionCheck svarInnConnectionCheck;
    @MockBean public AltinnConnectionCheck altinnConnectionCheck;
    @MockBean public CorrespondenceAgencyConnectionCheck correspondenceAgencyConnectionCheck;

    @Before
    @SneakyThrows
    public void before() {
        temporaryFolder.create();
    }

    @After
    public void after() {
        temporaryFolder.delete();
    }
}