package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.CertificateParser
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.ks.mapping.FiksMapper
import no.difi.meldingsutveksling.ks.svarut.SvarUtService
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceClient
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FiksTestConfiguration {

    @Bean
    CertificateParser certificateParser() {
        return new CertificateParser()
    }

    @Bean
    SvarUtService svarUtService(SvarUtWebServiceClient svarUtWebServiceClient,
                                ServiceRegistryLookup serviceRegistryLookup,
                                FiksMapper forsendelseMapper,
                                IntegrasjonspunktProperties props,
                                CertificateParser certificateParser) {
        return new SvarUtService(svarUtWebServiceClient, serviceRegistryLookup, forsendelseMapper, props, certificateParser)
    }

}
