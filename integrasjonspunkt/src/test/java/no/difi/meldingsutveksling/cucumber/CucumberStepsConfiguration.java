package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.Before;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.IntegrasjonspunktApplication;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamed;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
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
    public static class SpringConfiguration {

        @Bean
        public CucumberKeyStore cucumberKeyStore(IntegrasjonspunktProperties properties) {
            return new CucumberKeyStore(properties.getOrg().getKeystore());
        }

        @Bean
        public Holder<StandardBusinessDocument> standardBusinessDocumentHolder() {
            return new Holder<>();
        }

        @Bean
        public Holder<TransportInput> transportInputHolder() {
            return new Holder<>();
        }

        @Bean
        public Holder<AsicInfo> asicInfoHolder() {
            return new Holder<>();
        }

        @Bean
        @Primary
        public Transport transport() {
            return mock(Transport.class);
        }

        @Bean
        @Primary
        public TransportFactory transportFactory(Transport transport) {
            TransportFactory factory = mock(TransportFactory.class);
            given(factory.createTransport(any())).willReturn(transport);
            return factory;
        }

        @Bean
        public MockServerRestTemplateCustomizer mockServerRestTemplateCustomizer() {
            return new MockServerRestTemplateCustomizer(UnorderedRequestExpectationManager.class);
        }
    }

    @MockBean private IBrokerServiceExternalBasicStreamed streamingService;

    @Before
    @SneakyThrows
    public void before() {
    }
}