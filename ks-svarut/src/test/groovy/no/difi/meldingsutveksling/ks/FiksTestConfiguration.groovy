package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
public class FiksTestConfiguration {
    @Bean
    SvarUtService svarUtService(SvarUtWebServiceClient svarUtWebServiceClient, ServiceRegistryLookup serviceRegistryLookup, EDUCoreConverter eduCoreConverter) {
        return new SvarUtService(svarUtWebServiceClient, serviceRegistryLookup, eduCoreConverter)
    }

}
