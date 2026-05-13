package no.difi.meldingsutveksling.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.CertificateParserException;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.dph.DphAppStartupRunner;
import no.difi.meldingsutveksling.dph.DphService;
import no.difi.meldingsutveksling.dph.client.DigdirBusinessCertificateSupplier;
import no.difi.meldingsutveksling.dph.client.DphClient;
import no.difi.meldingsutveksling.dph.client.DphClientConfig;
import no.difi.meldingsutveksling.dph.client.internal.DphParcelService;
import no.difi.meldingsutveksling.nextmove.DphConversationStrategyImpl;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryClient;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

@Import(DphClientConfig.class)
@Configuration
@ConditionalOnProperty(name = "difi.move.feature.enableDPH", havingValue = "true")
@RequiredArgsConstructor
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class DphConfig {

    @Bean
    public DphAppStartupRunner dphAppStartupRunner(
        DphClient dphClient,
        IntegrasjonspunktProperties properties,
        ServiceRegistryLookup serviceRegistryLookup) {
        return new DphAppStartupRunner(dphClient, properties, serviceRegistryLookup);
    }

    @Bean
    public DphService dphService(ServiceRegistryLookup serviceRegistryLookup) {
        return new DphService(serviceRegistryLookup);
    }

    @Bean
    public DphConversationStrategyImpl dphConversationStrategy(
        DphClient dphClient,
        DphService dphService,
        DphParcelService dphParcelService,
        ConversationService conversationService,
        @Lazy NextMoveMessageService nextMoveMessageService) {
        return new DphConversationStrategyImpl(dphClient, dphService, dphParcelService, conversationService, nextMoveMessageService);
    }

    @Bean
    public DigdirBusinessCertificateSupplier digdirBusinessCertificateSupplier(ServiceRegistryClient serviceRegistryClient) {
        return () -> {
            try {
                return CertificateParser.parse(serviceRegistryClient.getCertificate("991825827"));
            } catch (ServiceRegistryLookupException e) {
                throw new NextMoveRuntimeException("Could not fetch DigDir certificate!");
            } catch (CertificateParserException e) {
                throw new NextMoveRuntimeException("Could not parse DigDir certificate!");
            }
        };
    }
}
