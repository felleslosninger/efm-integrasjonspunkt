package no.difi.meldingsutveksling.ks.svarinn;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.FiksMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@ConditionalOnProperty(name = "difi.move.fiks.inn.enable", havingValue = "true")
@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
@ComponentScan(basePackageClasses = {
        FiksMapper.class,
        SvarInnService.class
})
public class SvarInnConfiguration {

}