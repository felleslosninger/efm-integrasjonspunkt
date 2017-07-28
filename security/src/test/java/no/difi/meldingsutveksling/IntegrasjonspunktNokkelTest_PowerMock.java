package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.PathResource;

import java.security.KeyStore;

import static org.mockito.Mockito.*;

/**
 * Created by Even
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({KeyStore.class, IntegrasjonspunktNokkel.class})
public class IntegrasjonspunktNokkelTest_PowerMock {

    @Mock
    KeyStore.Builder mockBuilderWith3;

    @Mock
    KeyStore.Builder mockBuilderWith4;

    @Mock
    KeyStore keyStore;

    @Before
    public void before()throws Exception{

        PowerMockito.mockStatic(KeyStore.Builder.class);

        when(KeyStore.Builder.newInstance(any(), any(), any())).thenReturn(mockBuilderWith3);
        when(KeyStore.Builder.newInstance(any(), any(), any(), any())).thenReturn(mockBuilderWith4);

        when(mockBuilderWith3.getKeyStore()).thenReturn(keyStore);
        when(mockBuilderWith4.getKeyStore()).thenReturn(keyStore);
    }

    @Test
    public void testLoadKeyStoreWithoutPath()throws Exception{

        IntegrasjonspunktProperties.Keystore properties = new IntegrasjonspunktProperties.Keystore();
        properties.setAlias("alias");
        properties.setPassword("password");
        properties.setType("type");

        new IntegrasjonspunktNokkel(properties);

        PowerMockito.verifyStatic(never());
        KeyStore.Builder.newInstance(any(), any(), any(), any());

        PowerMockito.verifyStatic(times(1));
        KeyStore.Builder.newInstance(any(), any(), any());
    }

    @Test
    public void testLoadKeyStoreWithNONEPath()throws Exception{

        IntegrasjonspunktProperties.Keystore properties = new IntegrasjonspunktProperties.Keystore();
        properties.setAlias("alias");
        properties.setPassword("password");
        properties.setType("type");

        properties.setPath(new PathResource("NONE"));

        new IntegrasjonspunktNokkel(properties);

        PowerMockito.verifyStatic(never());
        KeyStore.Builder.newInstance(any(), any(), any(), any());

        PowerMockito.verifyStatic(times(1));
        KeyStore.Builder.newInstance(any(), any(), any());
    }


    @Test
    public void testLoadKeyStoreWithPath() throws Exception{

        IntegrasjonspunktProperties.Keystore properties = new IntegrasjonspunktProperties.Keystore();
        properties.setAlias("alias");
        properties.setPassword("password");
        properties.setType("type");
        properties.setPath(new PathResource("foo"));

        new IntegrasjonspunktNokkel(properties);

        PowerMockito.verifyStatic(never());
        KeyStore.Builder.newInstance(any(), any(), any());

        PowerMockito.verifyStatic(times(1));
        KeyStore.Builder.newInstance(any(), any(), any(), any());
    }
}
