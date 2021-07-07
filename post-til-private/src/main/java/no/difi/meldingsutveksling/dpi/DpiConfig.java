package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.xmlsoap.*;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;

import java.time.Clock;

@Configuration
@ConditionalOnProperty(name = "difi.move.feature.enableDPI", havingValue = "true")
public class DpiConfig {

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
