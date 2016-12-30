package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.HandlerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class SvarUtConfiguration {
    private IntegrasjonspunktProperties properties;

    public SvarUtConfiguration(IntegrasjonspunktProperties properties) {
        this.properties = properties;
    }

    @Bean
    public EDUCoreConverter eduCoreConverter(HandlerFactory handlerFactory) {
        return new EDUCoreConverterImpl(handlerFactory);
    }

    @Bean
    public HandlerFactory handlerFactory(IntegrasjonspunktProperties properties) {
        return new HandlerFactory(properties);
    }


    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(Forsendelse.class.getPackage().getName());
        return marshaller;
    }

}