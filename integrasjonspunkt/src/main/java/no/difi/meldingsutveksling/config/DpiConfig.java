package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.json.JsonDpiReceiptMapper;
import no.difi.meldingsutveksling.dpi.json.JsonMeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.json.ShipmentFactory;
import no.difi.meldingsutveksling.dpi.xmlsoap.*;
import no.difi.meldingsutveksling.nextmove.DpiConversationStrategyImpl;
import no.difi.meldingsutveksling.nextmove.MeldingsformidlerRequestFactory;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.status.DpiReceiptService;
import no.difi.meldingsutveksling.status.DpiStatusPolling;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.digdir.dpi.client.DpiClient;
import no.digdir.dpi.client.DpiClientConfig;
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
        DpiConfig.XmlSoap.class,
        DpiConfig.Json.class
})
public class DpiConfig {

    @Bean
    public DpiStatusPolling dpiStatusPolling(IntegrasjonspunktProperties properties, DpiReceiptService dpiReceiptService) {
        return new DpiStatusPolling(properties, dpiReceiptService);
    }

    @Bean
    public DpiReceiptService dpiReceiptService(IntegrasjonspunktProperties properties,
                                               MeldingsformidlerClient meldingsformidlerClient,
                                               ConversationService conversationService) {
        return new DpiReceiptService(properties, meldingsformidlerClient, conversationService);
    }

    @Order
    @Bean
    public DpiConversationStrategyImpl dpiConversationStrategyImpl(
            ServiceRegistryLookup sr,
            MeldingsformidlerRequestFactory meldingsformidlerRequestFactory,
            MeldingsformidlerClient meldingsformidlerClient,
            ConversationService conversationService,
            PromiseMaker promiseMaker

    ) {
        return new DpiConversationStrategyImpl(sr, meldingsformidlerRequestFactory, meldingsformidlerClient, conversationService, promiseMaker);
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

    @ConditionalOnProperty(name = "difi.move.dpi.client.type", havingValue = "json")
    @Import(DpiClientConfig.class)
    public static class Json {

        @Bean
        public MeldingsformidlerClient meldingsformidlerClient(DpiClient dpiClient,
                                                               ShipmentFactory shipmentFactory,
                                                               JsonDpiReceiptMapper dpiReceiptMapper) {
            return new JsonMeldingsformidlerClient(dpiClient, shipmentFactory, dpiReceiptMapper);
        }

        @Bean
        public ShipmentFactory shipmentFactory() {
            return new ShipmentFactory();
        }

        @Bean
        public JsonDpiReceiptMapper jsonDpiReceiptMapper(MessageStatusFactory messageStatusFactory) {
            return new JsonDpiReceiptMapper(messageStatusFactory);
        }
    }
}
