package no.difi.meldingsutveksling.altinnv3.resource;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.UseFullTestConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.AltinnConfiguration;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
@SpringBootTest(classes = {
    ResourceApiClient.class,
    ResourceTokenProducer.class,
    AltinnConfiguration.class,
    IntegrasjonspunktProperties.class
})
@UseFullTestConfiguration
public class ManuallyTestingResourceRegistry {

    @Inject
    ResourceApiClient client;

    @Inject
    ResourceTokenProducer resourceTokenProducer;

    @Inject
    IntegrasjonspunktProperties integrasjonspunktProperties;

    @Test
    void testResourceOwner() {
        String list = client.resourceOwner();
        assertNotNull(list, "List should not be null");
        System.out.println(list);
    }

    @Test
    void testResourceList() {
        String list = client.resourceList();
        assertNotNull(list, "List should not be null");
        System.out.println(list);
    }

    @Test
    void testAccessLists() {
        String list = client.accessLists();
        assertNotNull(list, "Resource list should not be null");
        System.out.println(list);
    }

    @Test
    void testProperties() {
        assertEquals("991825827", integrasjonspunktProperties.getOrg().getNumber(), "Finner ikke kjent organisasjon!");
    }

    @Test
    void testAltinnToken() {
        var altinnToken = resourceTokenProducer.produceToken(List.of("altinn:broker.write","altinn:broker.read","altinn:serviceowner"));
        assertNotNull(altinnToken, "AltinnToken is null");
    }

}
