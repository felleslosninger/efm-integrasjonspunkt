package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.DpiReceiptHandler;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.dpi.client.DpiClient;
import no.difi.meldingsutveksling.dpi.client.DpiClientConfig;
import no.difi.meldingsutveksling.dpi.client.internal.UnpackJWT;
import no.difi.meldingsutveksling.dpi.client.internal.UnpackStandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.json.*;
import no.difi.meldingsutveksling.nextmove.DpiConversationStrategyImpl;
import no.difi.meldingsutveksling.nextmove.MeldingsformidlerRequestFactory;
import no.difi.meldingsutveksling.nextmove.PrintService;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.status.*;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.SchedulingTaskExecutor;

import jakarta.xml.bind.JAXBException;
import java.time.Clock;

@Configuration
@ConditionalOnProperty(name = "difi.move.feature.enableDPI", havingValue = "true")
@Import({
        DpiConfig.Json.class
})
public class DpiConfig {

    @Bean
    public DigitalPostInnbyggerConfig digitalPostInnbyggerConfig(IntegrasjonspunktProperties properties) {
        return properties.getDpi();
    }

    @Bean
    public DpiStatusPolling dpiStatusPolling(IntegrasjonspunktProperties properties,
                                             SchedulingTaskExecutor dpiReceiptExecutor,
                                             DpiReceiptService dpiReceiptService) {
        return new DpiStatusPolling(properties, dpiReceiptExecutor, dpiReceiptService);
    }

    @Bean
    public DpiReceiptHandler dpiReceiptHandler(ConversationService conversationService,
                                               IntegrasjonspunktProperties properties) {
        return new DpiReceiptHandler(conversationService, properties);
    }

    @Bean
    public DpiReceiptService dpiReceiptService(MeldingsformidlerClient meldingsformidlerClient,
                                               DpiReceiptHandler dpiReceiptHandler,
                                               AvsenderidentifikatorHolder avsenderidentifikatorHolder) {
        return new DpiReceiptService(meldingsformidlerClient, dpiReceiptHandler, avsenderidentifikatorHolder);
    }

    @Order
    @Bean
    public DpiConversationStrategyImpl dpiConversationStrategyImpl(
            ServiceRegistryLookup sr,
            MeldingsformidlerRequestFactory meldingsformidlerRequestFactory,
            MeldingsformidlerClient meldingsformidlerClient,
            ConversationService conversationService,
            PrintService printService,
            PromiseMaker promiseMaker

    ) {
        return new DpiConversationStrategyImpl(sr, meldingsformidlerRequestFactory, meldingsformidlerClient, conversationService, printService, promiseMaker);
    }

    @Bean
    public AvsenderidentifikatorHolder avsenderindikatorHolder(IntegrasjonspunktProperties properties) {
        return new AvsenderidentifikatorHolder(properties);
    }

    @Bean
    public MpcIdHolder mpcIdHolder(IntegrasjonspunktProperties properties) {
        return new MpcIdHolder(properties);
    }

    @Bean
    public MeldingsformidlerRequestFactory meldingsformidlerRequestFactory(
            IntegrasjonspunktProperties properties,
            Clock clock,
            OptionalCryptoMessagePersister optionalCryptoMessagePersister,
            MpcIdHolder mpcIdHolder
    ) {
        return new MeldingsformidlerRequestFactory(properties, clock, optionalCryptoMessagePersister, mpcIdHolder);
    }

    @Import(DpiClientConfig.class)
    public static class Json {

        @Bean
        public MeldingsformidlerClient meldingsformidlerClient(MeldingsformidlerClient jsonMeldingsformidlerClient) {
            return jsonMeldingsformidlerClient;
        }

        @Bean
        public JsonMeldingsformidlerClient jsonMeldingsformidlerClient(DpiClient dpiClient,
                                                                       ShipmentFactory shipmentFactory,
                                                                       JsonDpiReceiptMapper dpiReceiptMapper,
                                                                       MessageStatusMapper messageStatusMapper,
                                                                       ChannelNormalizer channelNormalizer,
                                                                       DpiReceiptConverter dpiReceiptConverter,
                                                                       DpiMessageStatusFilter dpiMessageStatusFilter) {
            return new JsonMeldingsformidlerClient(dpiClient,
                    shipmentFactory, dpiReceiptMapper, messageStatusMapper,
                    channelNormalizer, dpiReceiptConverter, dpiMessageStatusFilter);
        }

        @Bean
        public ShipmentFactory shipmentFactory(ChannelNormalizer channelNormalizer) {
            return new ShipmentFactory(channelNormalizer);
        }

        @Bean
        public ChannelNormalizer channelNormalizer() {
            return new ChannelNormalizer();
        }

        @Bean
        public MessageStatusMapper messageStatusMapper(MessageStatusFactory messageStatusFactory) {
            return new MessageStatusMapper(messageStatusFactory);
        }

        @Bean
        public JsonDpiReceiptMapper jsonDpiReceiptMapper(MessageStatusMapper messageStatusMapper) {
            return new JsonDpiReceiptMapper(messageStatusMapper);
        }

        @Configuration
        @ConditionalOnProperty(name = "difi.move.dpi.receipt-type", havingValue = "json", matchIfMissing = true)
        public static class ReceiptTypeJson {

            @Bean
            public DpiMessageStatusFilter passThroughDpiMessageStatusFilter() {
                return p -> true;
            }

            @Bean
            public DpiReceiptConverter identityDpiReceiptConverter() {
                return p -> p;
            }

            @Bean
            public MessageStatusDecorator messageStatusNoActionDecorator() {
                return (c, p) -> p;
            }
        }

        @Configuration
        @ConditionalOnProperty(name = "difi.move.dpi.receipt-type", havingValue = "xmlsoap")
        public static class RecieptTypeXmlSoap {

            @Bean
            public XmlSoapDpiMessageStatusFilter xmlXmlSoapDpiMessageStatusFilter() {
                return new XmlSoapDpiMessageStatusFilter();
            }

            @Bean
            public JWT2XmlSoapDpiReceiptConverter jwt2XmlSoapDpiReceiptConverter(UnpackJWT unpackJWT,
                                                                                 UnpackStandardBusinessDocument unpackStandardBusinessDocument) throws JAXBException {
                return new JWT2XmlSoapDpiReceiptConverter(unpackJWT, unpackStandardBusinessDocument);
            }

            @Bean
            public MessageStatusRawReceiptXmlDecorator messageStatusRawReceiptXmlDecorator() throws JAXBException {
                return new MessageStatusRawReceiptXmlDecorator();
            }

        }
    }

}
