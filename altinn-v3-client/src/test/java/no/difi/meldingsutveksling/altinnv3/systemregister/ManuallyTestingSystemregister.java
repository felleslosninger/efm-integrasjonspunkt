package no.difi.meldingsutveksling.altinnv3.systemregister;

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
    SystemregisterApiClient.class,
    ServiceregisterTokenProducer.class,
    AltinnConfiguration.class,
    IntegrasjonspunktProperties.class
})
@UseFullTestConfiguration
public class ManuallyTestingSystemregister {

    @Inject
    SystemregisterApiClient client;

    @Inject
    ServiceregisterTokenProducer tokenProducer;

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
    void testSystemDetails() {
        String list = client.systemDetails();
        assertNotNull(list, "List should not be null");
        System.out.println(list);
    }

}
