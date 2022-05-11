package no.difi.meldingsutveksling.noark;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.mail.MailClient;
import no.difi.meldingsutveksling.noarkexchange.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NoarkClientFactoryTest {

    @Test
    public void config_noarkType_is_case_insensitive() {
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");
        NoarkClientFactory f = new NoarkClientFactory(settings);
        IntegrasjonspunktProperties properties = mock(IntegrasjonspunktProperties.class);
        IntegrasjonspunktProperties.NorskArkivstandardSystem noarkSystem = mock(IntegrasjonspunktProperties.NorskArkivstandardSystem.class);
        when(noarkSystem.getType()).thenReturn("P360").thenReturn("ePhOrTe").thenReturn("wEbSaK").thenReturn("mAIL");
        when(properties.getNoarkSystem()).thenReturn(noarkSystem);
        assertEquals(P360Client.class, f.from(properties).getClass());
        assertEquals(EphorteClient.class, f.from(properties).getClass());
        assertEquals(WebsakClient.class, f.from(properties).getClass());
        assertEquals(MailClient.class, f.from(properties).getClass());
    }

    @Test
    public void config_specifies_p360_creates_p360_client() {
        IntegrasjonspunktProperties props = mock(IntegrasjonspunktProperties.class);
        IntegrasjonspunktProperties.NorskArkivstandardSystem noarkSystem = mock(IntegrasjonspunktProperties
                .NorskArkivstandardSystem.class);
        when(noarkSystem.getType()).thenReturn("P360");
        when(props.getNoarkSystem()).thenReturn(noarkSystem);
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        NoarkClient client = new NoarkClientFactory(settings).from(props);

        assertEquals(P360Client.class, client.getClass());
    }

    @Test
    public void config_specifies_ephorte_create_ephorte_client() {
        IntegrasjonspunktProperties props = mock(IntegrasjonspunktProperties.class);
        IntegrasjonspunktProperties.NorskArkivstandardSystem noarkSystem = mock(IntegrasjonspunktProperties
                .NorskArkivstandardSystem.class);
        when(noarkSystem.getType()).thenReturn("ePhorte");
        when(props.getNoarkSystem()).thenReturn(noarkSystem);
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        NoarkClient client = new NoarkClientFactory(settings).from(props);

        assertEquals(EphorteClient.class, client.getClass());
    }

    @Test
    public void config_specifies_websak_create_websak_client() {
        IntegrasjonspunktProperties props = mock(IntegrasjonspunktProperties.class);
        IntegrasjonspunktProperties.NorskArkivstandardSystem noarkSystem = mock(IntegrasjonspunktProperties
                .NorskArkivstandardSystem.class);
        when(noarkSystem.getType()).thenReturn("WebSak");
        when(props.getNoarkSystem()).thenReturn(noarkSystem);
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        NoarkClient client = new NoarkClientFactory(settings).from(props);

        assertEquals(WebsakClient.class, client.getClass());
    }

    @Test
    public void config_specifies_unknownarchive_throws_exception() {
        IntegrasjonspunktProperties props = mock(IntegrasjonspunktProperties.class);
        IntegrasjonspunktProperties.NorskArkivstandardSystem noarkSystem = mock(IntegrasjonspunktProperties
                .NorskArkivstandardSystem.class);
        when(noarkSystem.getType()).thenReturn("UNKNOWNARCHIVESYSTEM");
        when(props.getNoarkSystem()).thenReturn(noarkSystem);
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        assertThrows(UnknownArchiveSystemException.class, () -> new NoarkClientFactory(settings).from(props));
    }

    @Test
    public void config_missing_throws_exception() {
        IntegrasjonspunktProperties properties = mock(IntegrasjonspunktProperties.class);
        NoarkClientSettings settings = new NoarkClientSettings("http://localhost", "username", "password");

        assertThrows(MissingConfigurationException.class, () -> new NoarkClientFactory(settings).from(properties));
    }
}
