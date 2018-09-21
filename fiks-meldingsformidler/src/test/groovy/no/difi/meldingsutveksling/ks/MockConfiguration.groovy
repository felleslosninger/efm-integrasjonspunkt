package no.difi.meldingsutveksling.ks

import com.google.common.collect.Maps
import no.difi.meldingsutveksling.noarkexchange.NoarkClient
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecordWrapper
import org.assertj.core.util.Lists
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
        when(lookup.getServiceRecord(Mockito.any(String), Mockito.eq(DPF))).thenReturn(Optional.of(new ServiceRecord(DPF, "123456789", pem, "http://localhost")))
        when(lookup.getServiceRecord(Mockito.any(String))).thenReturn(ServiceRecordWrapper.of(new ServiceRecord(DPF, "123456789", pem, "http://localhost"), Lists.newArrayList(), Maps.newHashMap()))
        def infoRecord = new InfoRecord("123456789", "foo", new EntityType("Organisasjonsledd", "ORGL"))
        when(lookup.getInfoRecord(Mockito.any(String))).thenReturn(infoRecord)
        return lookup
    }

    @Bean(name = "localNoark")
    public NoarkClient noarkClient() {
        return mock(NoarkClient)
    }

    @Bean(name = "fiksMailClient")
    NoarkClient fiksMailCLient() {
        return mock(NoarkClient)
    }
}
