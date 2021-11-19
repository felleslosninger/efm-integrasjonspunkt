import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.noarkexchange.DefaultTemplateFactory;
import no.difi.meldingsutveksling.noarkexchange.NoarkClientSettings;
import no.difi.meldingsutveksling.noarkexchange.NtlmTemplateFactory;
import no.difi.meldingsutveksling.noarkexchange.WebServiceTemplateFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NoarkClientSettingsTest {

    public static final LogstashMarker NO_MARKER = null;

    @Test
    public void userNamePasswordAndDomainCreatesNtlmAuthenticationFactory() throws Exception {
        NoarkClientSettings clientSettings = new NoarkClientSettings("http://localhost", "username", "password", "domain");

        WebServiceTemplateFactory templateFactory = clientSettings.createTemplateFactory();

        assertEquals(NtlmTemplateFactory.class, templateFactory.getClass());
    }

    @Test
    public void missingCredentialsCreatesDefaultTemplateFactory() {
        NoarkClientSettings clientSettings = new NoarkClientSettings("http://localhost", "", "");

        WebServiceTemplateFactory templateFactory = clientSettings.createTemplateFactory();

        assertEquals(DefaultTemplateFactory.class, templateFactory.getClass());
    }
}