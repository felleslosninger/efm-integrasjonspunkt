package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasic;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamed;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceClient;
import no.difi.meldingsutveksling.nextmove.ServiceBusRestTemplate;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.vefa.peppol.lookup.LookupClient;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

import javax.xml.bind.Marshaller;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ContextConfiguration(classes = {
        IntegrasjonspunktApplication.class,
        CucumberStepsConfiguration.SpringConfiguration.class
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
    public static class SpringConfiguration {

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
        public RequestCaptureClientInterceptor requestCaptureClientInterceptor(Holder<List<String>> webServicePayloadHolder) {
            return new RequestCaptureClientInterceptor(webServicePayloadHolder);
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
        public Clock clock() {
            return Clock.fixed(Instant.parse("2019-03-25T11:38:23Z"), ZoneId.of("Europe/Oslo"));
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

        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
        @Bean
        @Primary
        public AltinnWsClientFactory altinnWsClientFactory(
                IBrokerServiceExternalBasic iBrokerServiceExternalBasic,
                IBrokerServiceExternalBasicStreamed iBrokerServiceExternalBasicStreamed
        ) {
            return new AltinnWsClientFactory() {
                @Override
                public AltinnWsClient getAltinnWsClient(ServiceRecord serviceRecord) {
                    AltinnWsConfiguration configuration = AltinnWsConfiguration.fromConfiguration(serviceRecord, getApplicationContext());
                    return new AltinnWsClient(
                            iBrokerServiceExternalBasic,
                            iBrokerServiceExternalBasicStreamed,
                            configuration,
                            getApplicationContext());
                }
            };
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

    @MockBean public IBrokerServiceExternalBasic iBrokerServiceExternalBasic;
    @MockBean public IBrokerServiceExternalBasicStreamed iBrokerServiceExternalBasicStreamed;
    @MockBean public SvarUtWebServiceClient svarUtClient;
    @MockBean public UUIDGenerator uuidGenerator;
    @MockBean public LookupClient lookupClient;
    @MockBean public InternalQueue internalQueue;
    @MockBean public ServiceBusRestTemplate serviceBusRestTemplate;


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