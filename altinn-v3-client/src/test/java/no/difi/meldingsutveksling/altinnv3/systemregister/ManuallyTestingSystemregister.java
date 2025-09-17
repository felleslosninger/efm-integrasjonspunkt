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
    void testSystemuserToken() {
        var token = systemuserTokenProducer.produceToken(List.of("altinn:authentication/systemregister.write"));
        assertNotNull(token, "AltinnToken is null");
        var decodedToken = new String(Base64.getDecoder().decode(token.split("\\.")[1]));
        assertTrue(decodedToken.contains("\"client_id\":\"826acbbc-ee17-4946-af92-cf4885ebe951\""), "Systemuser token should contain correct client id");
    }

    @Test
    void testKnownClientToken() {
        var token = client.getTokenTest();
        var decodedToken = new String(Base64.getDecoder().decode(token.split("\\.")[1]));
        assertTrue(decodedToken.contains("\"systemuser_id\":[\"5b205bec-aad7-4e3f-a504-3e84b8a778fd\"]"), "Systemuser token should contain correct systemuser id");
        System.out.println(token);
    }

    @Test
    void testSystemDetails() {
        String list = client.systemDetails();
        assertNotNull(list, "List should not be null");
        System.out.println(list);
    }

    @Test
    void createSystem(){
        client.createSystem();
    }

    @Test
    void getAll(){
        var res = client.getAll();
        System.out.println(res);
    }

    @Test
    void getSystem(){
        var res = client.getSystem("314240979_integrasjonspunkt");
        System.out.println(res);
    }

    @Test
    void getSystemUsers(){
        var res = client.getAllSystemUsers("314240979_integrasjonspunkt");
        System.out.println(res);
    }

    @Test
    void updatePackagesForSystem(){
        var res = client.updateAccessPackage();
        System.out.println(res);
    }

    @Test
    void createAgentSystemUser(){
        var res = client.createAgentSystemUser();
        System.out.println(res);
    }

    @Test
    void createStandardSystemUser(){
        var res = client.createStandardSystemUser();
        System.out.println(res);
    }

}
