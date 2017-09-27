package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.config.KeyStoreProperties;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.security.KeyPair;

/**
 * Created by Even
 *
 * Manual test for integration against Windows CertificateStore
 *
 * Setup instructions:
 *
 * 1: Add certificate to the Windows CertificateStore
 * * Hint: you can convert the "/test.jks" keystore to a p12 format
 * * (with .p2 extension) and easily add it to the store by "executing" it.
 * * You can also open the CertificateManager by running "certmgr.msc" and
 * * navigating to "Personal".
 * * NOTE: The writer of these tests are not responsible if you destroy anything
 * * important and have to run from the IT department. You have been warned!
 * 2: Give the certificate the "Friendly name" of 'ip-test'.
 * 3: Run tests manually.
 *
 */
public class IntegrasjonspunktNokkelTest_WindowsIT {

    private static IntegrasjonspunktNokkel integrasjonspunktNokkel;

    private static final String ALIAS = "ip-test";

    @BeforeClass
    public static void before()throws Exception{

        KeyStoreProperties properties = new KeyStoreProperties();
        properties.setAlias(ALIAS);
        properties.setType("Windows-MY");

        integrasjonspunktNokkel = new IntegrasjonspunktNokkel(properties);
    }

    @Test
    @Ignore
    public void testGetKeyPair(){

        KeyPair keyPair = integrasjonspunktNokkel.getKeyPair();

        Assert.assertNotNull(keyPair.getPrivate());
        Assert.assertNotNull(keyPair.getPublic());
    }

    @Test
    @Ignore
    public void testGetSignatureHelper() throws Exception{

        IntegrasjonspunktNokkelTest.performGetSignatureHelper(integrasjonspunktNokkel, ALIAS);
    }
}
