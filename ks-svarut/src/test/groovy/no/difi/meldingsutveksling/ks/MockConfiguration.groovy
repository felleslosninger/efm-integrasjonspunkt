package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

@Configuration
public class MockConfiguration {
    @Bean
    public ServiceRegistryLookup serviceRegistryLookup() {
        def lookup = mock(ServiceRegistryLookup)
        def pem = this.getClass().getClassLoader().getResource("difi-cert-test.pem").text
        when(lookup.getServiceRecord(Mockito.any(String))).thenReturn(new ServiceRecord("FIKS", "123456789", pem, "http://localhost"))
        return lookup
    }
}
