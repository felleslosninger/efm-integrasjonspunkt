package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

import java.security.PrivateKey;

import static junit.framework.Assert.assertNotNull;

@Ignore("Temporary ignored. Functionality is to be moved, reason queue handling.")
public class IngegrasjonspunktNokkelTest {

    private IntegrasjonspunktNokkel nokkel;

    @Before
    public void init() {
        IntegrasjonspunktProperties.Keystore keystore = new IntegrasjonspunktProperties.Keystore();

        keystore.setAlias("974720760");
        keystore.setPassword("changeit");
        keystore.setPath(new FileSystemResource("src/main/resources/test-certificates.jks"));

        nokkel = new IntegrasjonspunktNokkel(keystore);

    }

    @Test
    public void testLastingavprivatnokkelfraTestressurser() {

        PrivateKey key = nokkel.loadPrivateKey();
        assertNotNull(key.getFormat());
    }

}
