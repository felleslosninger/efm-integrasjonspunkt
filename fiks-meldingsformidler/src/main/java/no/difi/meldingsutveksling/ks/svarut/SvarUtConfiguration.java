package no.difi.meldingsutveksling.ks.svarut;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
@ConditionalOnBooleanProperty(name = "difi.move.feature.enableDPF")
public class SvarUtConfiguration {

    @Bean
    public SvarUtService svarUtService(SvarUtClient svarUtClient, PromiseMaker promiseMaker, ServiceRegistryLookup serviceRegistry, ForsendelseIdRepository forsendelseIdRepository) {
        return new SvarUtService(svarUtClient, promiseMaker, serviceRegistry, forsendelseIdRepository);
    }
}
