package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.Before;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.*;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasic;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamed;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.vefa.peppol.common.model.DocumentTypeIdentifier;
import no.difi.vefa.peppol.lookup.LookupClient;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.UnorderedRequestExpectationManager;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
    @SpyBean(IntegrasjonspunktProperties.class)
    public static class SpringConfiguration {

        @Bean
        @Primary
        public SenderReferenceGenerator senderReferenceGenerator() {
            return () -> "19efbd4c-413d-4e2c-bbc5-257ef4a65b38";
        }

        @Bean
        @Primary
        @SneakyThrows
        public LookupClient lookupClient() {
            LookupClient mock = mock(LookupClient.class);
            given(mock.getDocumentIdentifiers(any()))
                    .willReturn(Collections.singletonList(DocumentTypeIdentifier.of("urn:no:difi:meldingsutveksling:2.0")));
            return mock;
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
        public Holder<StandardBusinessDocument> standardBusinessDocumentHolder() {
            return new Holder<>();
        }

        @Bean
        public Holder<ZipContent> zipContentHolder() {
            return new Holder<>();
        }

        @Bean
        public Holder<Message> messageHolder() {
            return new Holder<>();
        }

        @Bean
        public MockServerRestTemplateCustomizer mockServerRestTemplateCustomizer() {
            return new MockServerRestTemplateCustomizer(UnorderedRequestExpectationManager.class);
        }
    }

    @MockBean
    public IBrokerServiceExternalBasic iBrokerServiceExternalBasic;

    @MockBean
    public IBrokerServiceExternalBasicStreamed iBrokerServiceExternalBasicStreamed;

    @MockBean
    public SvarUtWebServiceClient svarUtClient;

    @Before
    public void before() {
    }
}