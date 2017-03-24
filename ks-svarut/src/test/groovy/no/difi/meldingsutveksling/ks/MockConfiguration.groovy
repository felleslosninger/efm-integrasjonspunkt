package no.difi.meldingsutveksling.ks

import no.difi.meldingsutveksling.noarkexchange.NoarkClient
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import static no.difi.meldingsutveksling.ServiceIdentifier.DPF
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

@Configuration
public class MockConfiguration {
    @Bean
    public ServiceRegistryLookup serviceRegistryLookup() {
        def lookup = mock(ServiceRegistryLookup)
        def pem = this.getClass().getClassLoader().getResource("difi-cert-test.pem").text
        when(lookup.getServiceRecord(Mockito.any(String))).thenReturn(new ServiceRecord(DPF, "123456789", pem, "http://localhost"))
        def infoRecord = new InfoRecord("123456789", "foo", new EntityType("Organisasjonsledd", "ORGL"))
        when(lookup.getInfoRecord(Mockito.any(String))).thenReturn(infoRecord)
        return lookup
    }

    @Bean(name = "localNoark")
    public NoarkClient noarkClient() {
        return mock(NoarkClient)
    }
}
