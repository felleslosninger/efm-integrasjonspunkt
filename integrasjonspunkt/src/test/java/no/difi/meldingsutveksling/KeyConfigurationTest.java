package no.difi.meldingsutveksling;


import no.difi.meldingsutveksling.config.KeyConfiguration;
import org.junit.Test;

import java.security.PrivateKey;

import static junit.framework.Assert.assertNotNull;

public class KeyConfigurationTest {
    @Test
    public void testLastingavprivatnokkelfraTestressurser() {
        KeyConfiguration keyConfig = new KeyConfiguration("src/main/resources/test-certificates.jks", "974720760", "changeit");
        PrivateKey key = keyConfig.loadPrivateKey();
        assertNotNull(key.getFormat());
    }
}
