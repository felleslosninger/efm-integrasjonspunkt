package no.difi.meldingsutveksling.ks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
@EnableConfigurationProperties({SvarUtProperties.class})
public class SvarUtConfiguration {
    @Autowired
    private SvarUtProperties properties;

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(Forsendelse.class.getPackage().getName());
        return marshaller;
    }

}