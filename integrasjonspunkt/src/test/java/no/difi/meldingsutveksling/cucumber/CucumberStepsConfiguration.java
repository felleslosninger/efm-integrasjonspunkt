package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.Before;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.IntegrasjonspunktApplication;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamed;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.UnorderedRequestExpectationManager;

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
//    @SpyBean(IntegrasjonspunktProperties.class)
//    @SpyBean(LauncherServiceImpl.class)
    public static class SpringConfiguration {

//        @Bean
//        @Primary
//        PublicKey publicKey() {
//            return mock(PublicKey.class);
//        }

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