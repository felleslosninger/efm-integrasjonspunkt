package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRequiredPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class IntegrasjonspunktBeans {
    @Autowired
    private Environment environment;

    @Bean
    public IntegrasjonspunktConfiguration integrasjonspunktConfiguration() throws MeldingsUtvekslingRequiredPropertyException {
        return new IntegrasjonspunktConfiguration(environment);
    }
}
