package no.difi.meldingsutveksling.noark;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.P360Client;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class NoarkClientFactoryTest {

    @Test
    public void config_specifies_p360_creates_p360_client() throws Exception {
        // Given configuration specifies P360
        IntegrasjonspunktConfig config = mock(IntegrasjonspunktConfig.class);

        // When NoarkClientFactory creates client

        NoarkClient client = new NoarkClientFactory().from(config);
        // Then we should have a P360 client

        assertEquals(P360Client.class, client.getClass());
    }

    @Test
    public void config_specifies_ephorte_create_ephorte_client() {

    }

    @Test
    public void config_specifies_unknownarchive_throws_exception() {

    }

    @Test
    public void config_missing_throws_exception() {

    }
}