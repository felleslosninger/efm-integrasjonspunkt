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
    SystemregisterTokenProducer.class,
    SystemuserTokenProducer.class,
    AltinnConfiguration.class,
    IntegrasjonspunktProperties.class
})
@UseFullTestConfiguration
public class ManuallyTestingSystemregister {

    @Inject
    SystemregisterApiClient client;

    @Inject
    SystemregisterTokenProducer tokenProducer;

    @Inject
    SystemuserTokenProducer systemuserTokenProducer;

    @Inject
    IntegrasjonspunktProperties integrasjonspunktProperties;

    @Test
    void testProperties() {
        assertEquals("991825827", integrasjonspunktProperties.getOrg().getNumber(), "Finner ikke kjent organisasjon!");
    }

    @Test
    void testAltinnToken() {
        var token = tokenProducer.produceToken(List.of("altinn:serviceowner"));
        assertNotNull(token, "AltinnToken is null");
        var decodedToken = new String(Base64.getDecoder().decode(token.split("\\.")[1]));
        assertTrue(decodedToken.contains("\"urn:altinn:org\":\"digdir\""), "AltinnToken should contain digdir as the org claim");
    }

    @Test
    void testJwkToken() {
        var token = systemuserTokenProducer.produceToken(List.of("altinn:authentication/systemregister"));
        assertNotNull(token, "AltinnToken is null");
        var decodedToken = new String(Base64.getDecoder().decode(token.split("\\.")[1]));
        assertTrue(decodedToken.contains("\"client_id\":\"b590f149-d0ba-4fca-b367-bccd9e444a00\""), "Systemuser token should contain correct client id");
    }

    @Test
    void testKnownClientToken() {
        var token = client.getTokenTest();
        var decodedToken = new String(Base64.getDecoder().decode(token.split("\\.")[1]));
        assertTrue(decodedToken.contains("\"systemuser_id\":[\"19bed6de-092d-4f4e-9a8c-7b95efaa400c\"]"), "Systemuser token should contain correct systemuser id");
        System.out.println(token);
    }

    @Test
    void testSystemDetails() {
        var res = client.systemDetails("991825827_integrasjonspunkt");
        assertNotNull(res, "System details should not be null");
        System.out.println(res);
    }

    @Test
    void createSystem() {
        var res = client.createSystem("991825827", "DIGDIR", "a63cac91-3210-4c35-b961-5c7bf122345c");
        System.out.println(res);
    }

    @Test
    void getAllSystems() {
        var res = client.getAll();
        System.out.println(res);
    }

    @Test
    void getSystem() {
        var res = client.getSystem("991825827_integrasjonspunkt");
        System.out.println(res);
    }

    @Test
    void updatePackagesForSystem() {
        var res = client.updateAccessPackage("991825827_integrasjonspunkt", "urn:altinn:accesspackage:informasjon-og-kommunikasjon");
        System.out.println(res);
    }

    @Test
    void createStandardSystemUser() {
        // det fulle navn på blir "<orgno>_integrasjonspunkt_systembruker_<name>"
        var res = client.createStandardSystemUser("312797062", "991825827_integrasjonspunkt", "skilpadde", "urn:altinn:accesspackage:informasjon-og-kommunikasjon");
        System.out.println(res);
    }

    @Test
    void getAllSystemUsers() {
        var res = client.getAllSystemUsers("991825827_integrasjonspunkt");
        System.out.println(res);
    }

    @Test
    void getSystemUserDetails() {
        // for system 991825827_integrasjonspunkt :
        // 991825827 ca73c480-3a77-476e-9e94-2af6ee741586 991825827_integrasjonspunkt_systembruker_test
        // 312797062 42d647ae-b335-4e85-9866-a5f01dbd5266 991825827_integrasjonspunkt_systembruker_skilpadde
        var res = client.getSystemUser("312797062", "42d647ae-b335-4e85-9866-a5f01dbd5266");
        System.out.println(res);
    }

    @Test
    void deleteSystemUser() {
        // 991825827 ca73c480-3a77-476e-9e94-2af6ee741586 991825827_integrasjonspunkt_systembruker_test// 312797062 42d647ae-b335-4e85-9866-a5f01dbd5266 991825827_integrasjonspunkt_systembruker_skilpadde
        var res = client.deleteSystemUser("991825827", "ca73c480-3a77-476e-9e94-2af6ee741586");
        System.out.println(res);
    }

    @Test
    void createAgentSystemUser() {
        var res = client.createAgentSystemUser();
        System.out.println(res);
    }

}
