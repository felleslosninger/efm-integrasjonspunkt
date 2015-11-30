package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.services.AdresseregisterService;
import no.difi.meldingsutveksling.services.CertificateException;
import no.difi.virksert.client.VirksertClientException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MessageSenderTest {
    private MessageSender messageSender;

    @Mock IntegrasjonspunktConfig configMock;

    @Before
    public void setUp() {
        initMocks(this);

        messageSender = new MessageSender();
        IntegrasjonspunktConfig config = mock(IntegrasjonspunktConfig.class);
        messageSender.setConfiguration(config);
        IntegrasjonspunktNokkel integrasjonspunktNokkel = mock(IntegrasjonspunktNokkel.class);
        messageSender.setKeyInfo(integrasjonspunktNokkel);

    }

    @Test(expected = MessageSender.MessageContextException.class)
    public void shouldThrowMessageContextExceptionWhenMissingRecipientOrganizationNumber() throws MessageSender.MessageContextException {
        PutMessageRequestAdapter requestAdapter = mock(PutMessageRequestAdapter.class);
        when(requestAdapter.getRecieverPartyNumber()).thenReturn(null);

        messageSender.createMessageContext(requestAdapter);
    }

    @Test(expected = MessageSender.MessageContextException.class)
    public void shouldThrowMessageContextExceptionWhenMissingRecipientCertificate() throws MessageSender.MessageContextException {
        PutMessageRequestAdapter requestAdapter = mock(PutMessageRequestAdapter.class);
        when(requestAdapter.hasRecieverPartyNumber()).thenReturn(true);
        when(requestAdapter.getRecieverPartyNumber()).thenReturn("123");
        AdresseregisterService adresseregister = mock(AdresseregisterService.class);
        when(adresseregister.getCertificate("123")).thenThrow(new CertificateException("hello", new VirksertClientException("hello")));
        messageSender.setAdresseregister(adresseregister);

        messageSender.createMessageContext(requestAdapter);
    }

    @Test(expected = MessageSender.MessageContextException.class)
    public void shouldThrowMessageContextExceptionWhenMissingSenderCertificate() throws MessageSender.MessageContextException {
        PutMessageRequestAdapter requestAdapter = mock(PutMessageRequestAdapter.class);
        when(requestAdapter.hasRecieverPartyNumber()).thenReturn(true);
        when(requestAdapter.getSenderPartynumber()).thenReturn("321");
        AdresseregisterService adresseregisterService = mock(AdresseregisterService.class);
        when(adresseregisterService.getCertificate("321")).thenThrow(new CertificateException("hello", new VirksertClientException("hello")));
        messageSender.setAdresseregister(adresseregisterService);

        messageSender.createMessageContext(requestAdapter);
    }

}