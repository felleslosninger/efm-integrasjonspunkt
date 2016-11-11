package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktApplication;
import no.difi.meldingsutveksling.PutMessageObjectMother;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration test class for {@link IntegrasjonspunktImpl}.
 *
 * Mock overrides are configured in {@link IntegrasjonspunktIntegrationTestConfig}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = IntegrasjonspunktApplication.class, webEnvironment = RANDOM_PORT, properties = {"app.local.properties.enable=false"})
@ActiveProfiles("test")
public class IntegrasjonspunktImplIntegrationTest {

    @Autowired
    private IntegrasjonspunktImpl integrasjonspunkt;

    @Test
    public void sendMessageTest() {
        String orgNr= "1337";
        PutMessageRequestType request = PutMessageObjectMother.createMessageRequestType(orgNr);
        request.getEnvelope().getReceiver().setOrgnr(orgNr);
        PutMessageResponseType response = integrasjonspunkt.putMessage(request);
        assertEquals("OK", response.getResult().getType());
    }
}
