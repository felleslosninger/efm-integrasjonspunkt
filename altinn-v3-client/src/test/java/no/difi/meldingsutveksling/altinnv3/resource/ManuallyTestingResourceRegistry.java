package no.difi.meldingsutveksling.altinnv3.resource;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.UseFullTestConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.AltinnConfiguration;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    ResourceTokenProducer tokenProducer;

    @Inject
    IntegrasjonspunktProperties integrasjonspunktProperties;

    @Test
    void testProperties() {
        assertEquals("991825827", integrasjonspunktProperties.getOrg().getNumber(), "Finner ikke kjent organisasjon!");
    }

    @Test
    void testAltinnToken() {
        var altinnToken = tokenProducer.produceToken(List.of("altinn:serviceowner"));
        assertNotNull(altinnToken, "AltinnToken is null");
        var decodedToken = new String(Base64.getDecoder().decode(altinnToken.split("\\.")[1]));
        assertTrue(decodedToken.contains("\"urn:altinn:org\":\"digdir\""), "AltinnToken should contain digdir as the org claim");
    }

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
        assertNotNull(list, "List should not be null");
        System.out.println(list);
    }

    @Test
    void testAccessListMembers() {
        String list = client.showAccesslistMembers("eformidling-meldingsteneste-test-tilgangsliste");
        assertNotNull(list, "List should not be null");
        System.out.println(list);
    }

    @Test
    void testAddAccessListMember() {
        String list = client.addAccesslistMember("eformidling-meldingsteneste-test-tilgangsliste");
        assertNotNull(list, "List should not be null");
        System.out.println(list);
    }

}
