package no.difi.meldingsutveksling.noark;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.noarkexchange.EphorteClient;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.NoarkClientSettings;
import no.difi.meldingsutveksling.noarkexchange.P360Client;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NoarkClientFactoryTest {

    @Test
    public void config_specifies_p360_creates_p360_client() throws Exception {
        IntegrasjonspunktConfig config = mock(IntegrasjonspunktConfig.class);
        when(config.getNoarkType()).thenReturn("P360");
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        NoarkClient client = new NoarkClientFactory(settings).from(config);

        assertEquals(P360Client.class, client.getClass());
    }

    @Test
    public void config_specifies_ephorte_create_ephorte_client() {
        IntegrasjonspunktConfig config = mock(IntegrasjonspunktConfig.class);
        when(config.getNoarkType()).thenReturn("ePhorte");
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        NoarkClient client = new NoarkClientFactory(settings).from(config);

        assertEquals(EphorteClient.class, client.getClass());
    }

    @Test(expected = UnknownArchiveSystemException.class)
    public void config_specifies_unknownarchive_throws_exception() {
        IntegrasjonspunktConfig config = mock(IntegrasjonspunktConfig.class);
        when(config.getNoarkType()).thenReturn("UNKNOWNARCHIVESYSTEM");
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        new NoarkClientFactory(settings).from(config);
    }

    @Test(expected = MissingConfigurationException.class)
    public void config_missing_throws_exception() {
        IntegrasjonspunktConfig config = mock(IntegrasjonspunktConfig.class);
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        new NoarkClientFactory(settings).from(config);
    }
}