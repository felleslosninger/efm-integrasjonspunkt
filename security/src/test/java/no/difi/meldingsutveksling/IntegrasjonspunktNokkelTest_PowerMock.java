package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.config.KeyStoreProperties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KeyStore.class, IntegrasjonspunktNokkel.class, KeystoreProvider.class})
public class IntegrasjonspunktNokkelTest_PowerMock {

    @Mock
    KeyStore keyStore;

    @Mock
    Resource file;

    @Before
    public void before()throws Exception{
        PowerMockito.mockStatic(KeyStore.class);
        when(KeyStore.getInstance(anyString())).thenReturn(keyStore);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
    }

    @Test
    public void testLoadKeyStoreWithoutPath()throws Exception{

        KeyStoreProperties properties = new KeyStoreProperties();
        properties.setAlias("alias");
        properties.setPassword("password");
        properties.setType("type");

        IntegrasjonspunktNokkel integrasjonspunktNokkel = new IntegrasjonspunktNokkel(properties);

        Assert.assertEquals(keyStore, integrasjonspunktNokkel.getKeyStore());
    }

    @Test
    public void testLoadKeyStoreWithNONEPath()throws Exception{

        KeyStoreProperties properties = new KeyStoreProperties();
        properties.setAlias("alias");
        properties.setPassword("password");
        properties.setType("type");

        properties.setPath(new PathResource("NONE"));

        IntegrasjonspunktNokkel integrasjonspunktNokkel = new IntegrasjonspunktNokkel(properties);

        Assert.assertEquals(keyStore, integrasjonspunktNokkel.getKeyStore());
    }


    @Test
    public void testLoadKeyStoreWithPath() throws Exception{

        KeyStoreProperties properties = new KeyStoreProperties();
        properties.setAlias("alias");
        properties.setPassword("password");
        properties.setType("type");
        properties.setPath(file);

        IntegrasjonspunktNokkel integrasjonspunktNokkel = new IntegrasjonspunktNokkel(properties);

        Assert.assertEquals(keyStore, integrasjonspunktNokkel.getKeyStore());
    }
}
