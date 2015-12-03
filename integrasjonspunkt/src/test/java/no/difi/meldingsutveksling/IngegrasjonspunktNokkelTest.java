package no.difi.meldingsutveksling;


import org.junit.Ignore;
import org.junit.Test;

import java.security.PrivateKey;

import static junit.framework.Assert.assertNotNull;

@Ignore("Temporary ignored. Functionality is to be moved, reason queue handling.")
public class IngegrasjonspunktNokkelTest {


    @Test
    public void testLastingavprivatnokkelfraTestressurser() {

        IntegrasjonspunktNokkel nokkel = new IntegrasjonspunktNokkel("src/main/resources/test-certificates.jks", "974720760", "changeit");
        PrivateKey key = nokkel.loadPrivateKey();
        assertNotNull(key.getFormat());
    }

}
