package no.difi.meldingsutveksling.ks.svarut;

import no.difi.meldingsutveksling.CertificateParser;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.FiksMapper;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
@ComponentScan(basePackageClasses = {
        FiksMapper.class
})
public class SvarUtConfiguration {

    @Bean
    public CertificateParser certificateParser() {
        return new CertificateParser();
    }

    @Bean
    public SvarUtService svarUtService(FiksMapper fiksMapper,
                                       SvarUtWebServiceClient svarUtClient,
                                       ServiceRegistryLookup serviceRegistryLookup,
                                       IntegrasjonspunktProperties props,
                                       CertificateParser certificateParser) {
        return new SvarUtService(svarUtClient, serviceRegistryLookup, fiksMapper, props, certificateParser);
    }
}