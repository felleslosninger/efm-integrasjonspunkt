package no.difi.meldingsutveksling.noark;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.noarkexchange.EphorteClient;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
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

        NoarkClient client = new NoarkClientFactory().from(config);

        assertEquals(P360Client.class, client.getClass());
    }

    @Test
    public void config_specifies_ephorte_create_ephorte_client() {
        IntegrasjonspunktConfig config = mock(IntegrasjonspunktConfig.class);
        when(config.getNoarkType()).thenReturn("ePhorte");

        NoarkClient client = new NoarkClientFactory().from(config);

        assertEquals(EphorteClient.class, client.getClass());
    }

    @Test(expected = UnkownArchiveSystemException.class)
    public void config_specifies_unknownarchive_throws_exception() {
        IntegrasjonspunktConfig config = mock(IntegrasjonspunktConfig.class);
        when(config.getNoarkType()).thenReturn("UNKNOWNARCHIVESYSTEM");

        NoarkClient client = new NoarkClientFactory().from(config);
    }

    @Test(expected = MissingConfigurationException.class)
    public void config_missing_throws_exception() {
        IntegrasjonspunktConfig config = mock(IntegrasjonspunktConfig.class);

        NoarkClient client = new NoarkClientFactory().from(config);


    }
}