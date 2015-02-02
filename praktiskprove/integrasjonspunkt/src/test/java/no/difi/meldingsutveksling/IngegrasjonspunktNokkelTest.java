package no.difi.meldingsutveksling;


import no.difi.meldingsutveksling.oxalisexchange.IntegrasjonspunktNokkel;
import org.junit.Test;

import java.security.PrivateKey;

import static junit.framework.Assert.assertNotNull;

public class IngegrasjonspunktNokkelTest {


    @Test
    public void testLastingavprivatnokkelfraTestressurser() {

        IntegrasjonspunktNokkel nokkel = new IntegrasjonspunktNokkel("test-certificates.jks", "974720760", "changeit");
        PrivateKey key = nokkel.loadPrivateKey();
        assertNotNull(key.getFormat());
    }

}
