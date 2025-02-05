package no.difi.meldingsutveksling.cucumber;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.spring.CucumberContextConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.IntegrasjonspunktApplication;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.clock.TestClock;
import no.difi.meldingsutveksling.clock.TestClockConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.AsicParser;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnConnectionCheck;
import no.difi.meldingsutveksling.ks.svarut.SvarUtClientHolder;
import no.difi.meldingsutveksling.ks.svarut.SvarUtConnectionCheck;
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceClientImpl;
import no.difi.meldingsutveksling.nextmove.InternalQueue;
import no.difi.meldingsutveksling.nextmove.KrrPrintResponse;
import no.difi.meldingsutveksling.nextmove.PrintService;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusRestTemplate;
import no.difi.meldingsutveksling.noarkexchange.altinn.AltinnConnectionCheck;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.mapping.CorrespondenceAgencyConnectionCheck;
import no.difi.meldingsutveksling.webhooks.WebhookPusher;
import no.difi.move.common.cert.KeystoreHelper;
import no.ks.fiks.io.client.FiksIOKlient;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;

import java.io.File;
import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        IntegrasjonspunktApplication.class,
        TestClockConfig.class,
        CucumberStepsConfiguration.SpringConfiguration.class,
        CucumberStepsConfiguration.SvarUtConfiguration.class
}, loader = SpringBootContextLoader.class)
@CucumberContextConfiguration
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
    public static class SvarUtConfiguration {

        private final SvarUtClientHolder svarUtClientHolder;
        private final RequestCaptureClientInterceptor requestCaptureClientInterceptor;
        private final IntegrasjonspunktProperties properties;

        @PostConstruct
        public void configureSvarUtClient() {
            SvarUtWebServiceClientImpl client = svarUtClientHolder.getClient(properties.getOrg().getNumber());
            client.setInterceptors(ArrayUtils.add(client.getInterceptors(), requestCaptureClientInterceptor));
        }

    }

    @Configuration
    @Profile("cucumber")
    @RequiredArgsConstructor
    @SpyBean(WebhookPusher.class)
    @SpyBean(IntegrasjonspunktProperties.class)
    public static class SpringConfiguration {

        @Primary
        @Bean
        public UUIDGenerator uuidGenerator() {
            UUIDGenerator uuidGenerator = mock(UUIDGenerator.class);
            when(uuidGenerator.generate()).thenReturn("ff88849c-e281-4809-8555-7cd54952b921");
            return uuidGenerator;
        }

        @Primary
        @Bean
        public Clock clock(TestClock testClock) {
            return testClock;
        }

        @Bean
        @Primary
        InMemoryMessagePersister inMemoryMessagePersister() {
            return new InMemoryMessagePersister();
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
        public TaskScheduler taskScheduler() {
            return new NoopTaskScheduler();
        }

        @Bean
        public AsicParser asicParser() {
            return new AsicParser();
        }

        @Bean
        public KeystoreHelper fiksKeystoreHelper(IntegrasjonspunktProperties properties) {
            return new KeystoreHelper(properties.getFiks().getKeystore());
        }

        @Bean
        public PrintService printService() {
            PrintService mock = mock(PrintService.class);
            KrrPrintResponse printResponse = new KrrPrintResponse();
            printResponse.setPostkasseleverandoerAdresse("987464291");
            printResponse.setX509Sertifikat("-----BEGIN CERTIFICATE-----\nMIICujCCAaKgAwIBAgIEXIe4JzANBgkqhkiG9w0BAQsFADAeMRwwGgYDVQQDDBNE\nSUZJIHRlc3QgOTg3NDY0MjkxMCAXDTE5MDMxMjEzNDYxNVoYDzIxMTkwMzEyMTM0\nNjE1WjAeMRwwGgYDVQQDDBNESUZJIHRlc3QgOTg3NDY0MjkxMIIBIjANBgkqhkiG\n9w0BAQEFAAOCAQ8AMIIBCgKCAQEAz3fGUUZG9mQmiaXrY5j6EKofssPirvbkyqUD\n893jG2DGybjooSlsIub+NMJx3Dl+5jC9YIB6/BXglfReOg0LvvcoMR7Dr6rOCcKO\nWGMFhaCqlemEJ+HVCj6aOQ87lu+Zbb6hXTxkC1tTLd9x85hPXOH5x53MVzysj43e\nW+CG9VGXcwQBxksuyP+NRI8hEbwlCPcNjNg6u8X2akhKM4JyeaIpGdXNG2EmA0bd\nIaej6oAtZJ79x+3eR3MJR9TB5mauDZng9k5SxC9PxEENzhDaar8aXQrEFCFsRKEI\nnanOOvfihVoFeTxbsca6OeeSBaMNC11egRr+Ks1LsRCs+GF7AwIDAQABMA0GCSqG\nSIb3DQEBCwUAA4IBAQC9dlBI0kOkEQe6FCebfen38ns+kyqVk2I+xJ0MFxyJTd93\nF03BKv3y7WaYkYMBQBa2gyMqRCrH9q1ZNjKX4kjI/g0Hd7Kvqaup44kOaWrnA3pJ\n+5/OvGEdWdhrrKUrXhO7L7lbgu5jvX7emEpn1E8V+8WMDMHg1JMNNflJ0ZXgOU3e\n1tXkzuCBQWguDwdkoX923lUdGeD6h7SnTKzEvoXx2zHDQ0qUTl9W43vmCoxmmmhE\nzsJMnNUh8hK6NolPu7ZLPvkOEr+oLRKfDK6UR2pqVRJpvfCK9r+fDBTh1gmK144I\n6GRg5gVjSwEquF9GzTZ1PW8HNaxLsgA0EuVWZXfh\n-----END CERTIFICATE-----\n");
            when(mock.getPrintDetails()).thenReturn(printResponse);
            return mock;
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

    @TempDir
    File temporaryFolder;

    @MockitoBean public UUIDGenerator uuidGenerator;
    @MockitoBean public InternalQueue internalQueue;
    @MockitoBean public ServiceBusRestTemplate serviceBusRestTemplate;
    @MockitoBean public SvarUtConnectionCheck svarUtConnectionCheck;
    @MockitoBean public SvarInnConnectionCheck svarInnConnectionCheck;
    @MockitoBean public AltinnConnectionCheck altinnConnectionCheck;
    @MockitoBean public CorrespondenceAgencyConnectionCheck correspondenceAgencyConnectionCheck;
    @MockitoBean public FiksIOKlient fiksIOKlient;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS) public RestClient restClient;

}
