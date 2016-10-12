package no.difi.meldingsutveksling.noark;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.*;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NoarkClientFactoryTest {

    @Test
    public void config_noarkType_is_case_insensitive() {
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");
        NoarkClientFactory f = new NoarkClientFactory(settings);
        IntegrasjonspunktProperties.NorskArkivstandardSystem properties = mock(IntegrasjonspunktProperties.NorskArkivstandardSystem.class);
        when(properties.getType()).thenReturn("P360").thenReturn("ePhOrTe").thenReturn("wEbSaK");
        assertEquals(P360Client.class, f.from(properties).getClass());
        assertEquals(EphorteClient.class, f.from(properties).getClass());
        assertEquals(WebsakClient.class, f.from(properties).getClass());
    }

    @Test
    public void config_specifies_p360_creates_p360_client() throws Exception {
        IntegrasjonspunktProperties.NorskArkivstandardSystem properties = mock(IntegrasjonspunktProperties.NorskArkivstandardSystem.class);
        when(properties.getType()).thenReturn("P360");
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        NoarkClient client = new NoarkClientFactory(settings).from(properties);

        assertEquals(P360Client.class, client.getClass());
    }

    @Test
    public void config_specifies_ephorte_create_ephorte_client() {
        IntegrasjonspunktProperties.NorskArkivstandardSystem properties = mock(IntegrasjonspunktProperties.NorskArkivstandardSystem.class);
        when(properties.getType()).thenReturn("ePhorte");
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        NoarkClient client = new NoarkClientFactory(settings).from(properties);

        assertEquals(EphorteClient.class, client.getClass());
    }

    @Test
    public void config_specifies_websak_create_websak_client() {
        IntegrasjonspunktProperties.NorskArkivstandardSystem properties = mock(IntegrasjonspunktProperties.NorskArkivstandardSystem.class);
        when(properties.getType()).thenReturn("WebSak");
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        NoarkClient client = new NoarkClientFactory(settings).from(properties);

        assertEquals(WebsakClient.class, client.getClass());
    }

    @Test(expected = UnknownArchiveSystemException.class)
    public void config_specifies_unknownarchive_throws_exception() {
        IntegrasjonspunktProperties.NorskArkivstandardSystem properties = mock(IntegrasjonspunktProperties.NorskArkivstandardSystem.class);
        when(properties.getType()).thenReturn("UNKNOWNARCHIVESYSTEM");
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        new NoarkClientFactory(settings).from(properties);
    }

    @Test(expected = MissingConfigurationException.class)
    public void config_missing_throws_exception() {
        IntegrasjonspunktProperties.NorskArkivstandardSystem properties = mock(IntegrasjonspunktProperties.NorskArkivstandardSystem.class);
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        new NoarkClientFactory(settings).from(properties);
    }
}
