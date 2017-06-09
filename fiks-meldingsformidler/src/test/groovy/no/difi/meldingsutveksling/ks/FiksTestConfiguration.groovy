package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.ks.mapping.ForsendelseMapper
import no.difi.meldingsutveksling.ks.svarut.SvarUtService
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceClient
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FiksTestConfiguration {
    @Bean
    SvarUtService svarUtService(SvarUtWebServiceClient svarUtWebServiceClient, ServiceRegistryLookup serviceRegistryLookup, ForsendelseMapper forsendelseMapper) {
        return new SvarUtService(svarUtWebServiceClient, serviceRegistryLookup, forsendelseMapper)
    }

}
