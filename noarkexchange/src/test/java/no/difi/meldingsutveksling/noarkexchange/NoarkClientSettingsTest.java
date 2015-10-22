package no.difi.meldingsutveksling.noarkexchange;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NoarkClientSettingsTest {

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