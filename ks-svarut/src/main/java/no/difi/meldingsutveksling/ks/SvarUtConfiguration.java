package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.HandlerFactory;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class SvarUtConfiguration {

    @Bean
    public EDUCoreConverter eduCoreConverter(HandlerFactory handlerFactory) {
        return new EDUCoreConverterImpl(handlerFactory);
    }

    @Bean
    public HandlerFactory handlerFactory(IntegrasjonspunktProperties properties) {
        return new HandlerFactory(properties);
    }

    @Bean
    public SvarUtService svarUtService(EDUCoreConverter converter, SvarUtWebServiceClient svarUtClient, ServiceRegistryLookup serviceRegistryLookup) {
        return new SvarUtService(svarUtClient, serviceRegistryLookup, converter);
    }

}