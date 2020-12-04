package no.difi.meldingsutveksling;

import no.difi.move.common.config.KeystoreProperties;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

import java.security.PrivateKey;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Temporary ignored. Functionality is to be moved, reason queue handling.")
public class IngegrasjonspunktNokkelTest {

    private IntegrasjonspunktNokkel nokkel;

    @Before
    public void init() {
        KeystoreProperties keystore = new KeystoreProperties();

        keystore.setAlias("974720760");
        keystore.setPassword("changeit");
        keystore.setPath(new FileSystemResource("src/main/resources/test-certificates.jks"));

        nokkel = new IntegrasjonspunktNokkel(keystore);

    }

    @Test
    public void testLastingavprivatnokkelfraTestressurser() {

        PrivateKey key = nokkel.loadPrivateKey();
        assertThat(key.getFormat()).isNotNull();
    }

}
