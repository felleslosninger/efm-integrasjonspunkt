package no.difi.meldingsutveksling.ks


import no.difi.meldingsutveksling.noarkexchange.NoarkClient
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import org.mockito.Mockito
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

import static no.difi.meldingsutveksling.ServiceIdentifier.DPF
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

@Configuration
public class MockConfiguration {
    @Bean
    ServiceRegistryLookup serviceRegistryLookup() {
        def lookup = mock(ServiceRegistryLookup)
        def pem = this.getClass().getClassLoader().getResource("difi-cert-test.pem").text
        when(lookup.getServiceRecord(Mockito.any(String), Mockito.eq(DPF))).thenReturn(new ServiceRecord(DPF, "123456789", pem, "http://localhost"))
        when(lookup.getServiceRecord(Mockito.any(String))).thenReturn(new ServiceRecord(DPF, "123456789", pem, "http://localhost"))
        def infoRecord = new InfoRecord("123456789", "foo", new EntityType("Organisasjonsledd", "ORGL"))
        when(lookup.getInfoRecord(Mockito.any(String))).thenReturn(infoRecord)
        return lookup
    }

    @Bean
    RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder()
    }

    @Bean(name = "localNoark")
    public NoarkClient noarkClient() {
        return mock(NoarkClient)
    }

    @Bean(name = "fiksMailClient")
    public NoarkClient fiksMailCLient() {
        return mock(NoarkClient)
    }

    @Bean
    @Primary
    public Clock clock() {
        return Clock.fixed(Instant.parse("2019-03-25T11:38:23Z"), ZoneId.of("Europe/Oslo"))
    }
}
