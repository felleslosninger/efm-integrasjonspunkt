package no.difi.meldingsutveksling;

import java.security.PrivateKey;
import static junit.framework.Assert.assertNotNull;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;

@Ignore("Temporary ignored. Functionality is to be moved, reason queue handling.")
public class IngegrasjonspunktNokkelTest {

    private IntegrasjonspunktNokkel nokkel;

    @Before
    public void init() {
        IntegrasjonspunktProperties properties = new IntegrasjonspunktProperties();

        properties.setOrg(new IntegrasjonspunktProperties.Organization());
        properties.getOrg().setKeystore(new IntegrasjonspunktProperties.Keystore());

        properties.getOrg().getKeystore().setAlias("974720760");
        properties.getOrg().getKeystore().setPassword("changeit");
        properties.getOrg().getKeystore().setPath(new FileSystemResource("src/main/resources/test-certificates.jks"));

        nokkel = new IntegrasjonspunktNokkel(properties);

    }

    @Test
    public void testLastingavprivatnokkelfraTestressurser() {

        PrivateKey key = nokkel.loadPrivateKey();
        assertNotNull(key.getFormat());
    }

}
