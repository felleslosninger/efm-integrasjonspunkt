package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.xmlsoap.*;
import no.difi.meldingsutveksling.nextmove.DpiConversationStrategyImpl;
import no.difi.meldingsutveksling.nextmove.MeldingsformidlerRequestFactory;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

import java.time.Clock;

@Configuration
@ConditionalOnProperty(name = "difi.move.feature.enableDPI", havingValue = "true")
@Import({
        DpiConfig.XmlSoap.class
})
public class DpiConfig {

    @Order
    @Bean
    public DpiConversationStrategyImpl dpiConversationStrategyImpl(
            IntegrasjonspunktProperties props,
            ServiceRegistryLookup sr,
            Clock clock,
            OptionalCryptoMessagePersister optionalCryptoMessagePersister,
            MeldingsformidlerRequestFactory meldingsformidlerRequestFactory,
            MeldingsformidlerClient meldingsformidlerClient,
            ConversationService conversationService,
            PromiseMaker promiseMaker

    ) {
        return new DpiConversationStrategyImpl(props, sr, clock, optionalCryptoMessagePersister, meldingsformidlerRequestFactory, meldingsformidlerClient, conversationService, promiseMaker);
    }

    @Bean
    public MeldingsformidlerRequestFactory meldingsformidlerRequestFactory(
            IntegrasjonspunktProperties properties,
            Clock clock,
            OptionalCryptoMessagePersister optionalCryptoMessagePersister
    ) {
        return new MeldingsformidlerRequestFactory(properties, clock, optionalCryptoMessagePersister);
    }

    @ConditionalOnProperty(name = "difi.move.dpi.client.type", havingValue = "xmlsoap")
    public static class XmlSoap {

        @Bean
        public MeldingsformidlerClient meldingsformidlerClient(IntegrasjonspunktProperties properties,
                                                               SikkerDigitalPostKlientFactory sikkerDigitalPostKlientFactory,
                                                               ForsendelseHandlerFactory forsendelseHandlerFactory,
                                                               DpiReceiptMapper dpiReceiptMapper,
                                                               ClientInterceptor metricsEndpointInterceptor,
                                                               MetadataDocumentConverter metadataDocumentConverter) {
            return new XmlSoapMeldingsformidlerClient(properties.getDpi(), sikkerDigitalPostKlientFactory,
                    forsendelseHandlerFactory, dpiReceiptMapper, metricsEndpointInterceptor, metadataDocumentConverter);
        }

        @Bean
        public MetadataDocumentConverter metadataDocumentConverter() {
            return new MetadataDocumentConverter();
        }

        @Bean
        public ForsendelseHandlerFactory forsendelseHandlerFactory(IntegrasjonspunktProperties properties) {
            return new ForsendelseHandlerFactory(properties.getDpi());
        }

        @Bean
        public SikkerDigitalPostKlientFactory sikkerDigitalPostKlientFactory(IntegrasjonspunktProperties props) {
            return new SikkerDigitalPostKlientFactory(props);
        }

        @Bean
        public DpiReceiptMapper dpiReceiptMapper(MessageStatusFactory messageStatusFactory, Clock clock) {
            return new DpiReceiptMapper(messageStatusFactory, clock);
        }

    }

}
