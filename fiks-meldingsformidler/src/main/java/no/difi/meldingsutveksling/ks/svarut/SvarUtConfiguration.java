package no.difi.meldingsutveksling.ks.svarut;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.mapping.FiksMapper;
import no.difi.meldingsutveksling.ks.mapping.ForsendelseMapper;
import no.difi.meldingsutveksling.ks.mapping.ForsendelseStatusMapper;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@ConditionalOnProperty(name="difi.move.feature.enableDPF", havingValue = "true")
@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class SvarUtConfiguration {

    @Bean
    public FiksMapper fiksMapper(IntegrasjonspunktProperties properties,
                                 ServiceRegistryLookup serviceRegistryLookup,
                                 ObjectProvider<MessagePersister> persister) {
        final ForsendelseMapper forsendelseMapper = new ForsendelseMapper(properties, serviceRegistryLookup, persister.getIfUnique());
        return new FiksMapper(forsendelseMapper, new ForsendelseStatusMapper());
    }

    @Bean
    public SvarUtService svarUtService(FiksMapper fiksMapper,
                                       SvarUtWebServiceClient svarUtClient,
                                       ServiceRegistryLookup serviceRegistryLookup,
                                       IntegrasjonspunktProperties props) {
        return new SvarUtService(svarUtClient, serviceRegistryLookup, fiksMapper, props);
    }

}