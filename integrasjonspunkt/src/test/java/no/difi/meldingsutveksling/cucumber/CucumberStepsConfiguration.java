package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.Before;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.IntegrasjonspunktApplication;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.altinn.AltinnWsClientFactory;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
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
        public TaskScheduler poolScheduler() {
            return new NoopTaskScheduler();
        }

        @Bean
        public AsicParser asicParser() {
            return new AsicParser();
        }

        @Bean
        public AltinnWsClient altinnWsClient() {
            return mock(AltinnWsClient.class);
        }

        @Bean
        @Primary
        public AltinnWsClientFactory altinnWsClientFactory(AltinnWsClient altinnWsClient) {
            AltinnWsClientFactory factory = mock(AltinnWsClientFactory.class);
            given(factory.getAltinnWsClient(any())).willReturn(altinnWsClient);
            return factory;
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
        public Holder<UploadRequest> uploadRequestHolder() {
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

    @Before
    public void before() {
    }
}