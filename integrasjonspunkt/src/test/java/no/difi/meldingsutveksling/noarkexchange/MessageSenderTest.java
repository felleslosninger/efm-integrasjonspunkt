package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.services.AdresseregisterVirksert;
import no.difi.meldingsutveksling.services.CertificateException;
import no.difi.virksert.client.VirksertClientException;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageSenderTest {
    public static final String SENDER_PARTY_NUMBER = "910075918";
    public static final String RECIEVER_PARTY_NUMBER = "910077473";
    public static final String CONVERSATION_ID = "conversationId";
    public static final String JOURNALPOST_ID = "journalpostid";
    private MessageSender messageSender;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private AdresseregisterVirksert adresseregister;

    @Mock
    private IntegrasjonspunktConfiguration config;

    @Mock
    private IntegrasjonspunktNokkel integrasjonspunktNokkel;

    @Before
    public void setUp() {
        messageSender = new MessageSender();
        messageSender.setConfiguration(config);
        messageSender.setKeyInfo(integrasjonspunktNokkel);
        messageSender.setAdresseregister(adresseregister);
    }

    @Test
    public void shouldThrowMessageContextExceptionWhenMissingRecipientOrganizationNumber() throws MessageContextException {
        expectedException.expect(MessageContextException.class);
        expectedException.expect(new StatusMatches(StatusMessage.MISSING_RECIEVER_ORGANIZATION_NUMBER));
        PutMessageRequestWrapper requestAdapter = new RequestBuilder().withSender().build();

        messageSender.createMessageContext(requestAdapter);
    }

    @Test
    public void shouldThrowMessageContextExceptionWhenMissingRecipientCertificate() throws MessageContextException, CertificateException {
        expectedException.expect(MessageContextException.class);
        expectedException.expect(new StatusMatches(StatusMessage.MISSING_RECIEVER_CERTIFICATE));
        PutMessageRequestWrapper requestAdapter = new RequestBuilder().withSender().withReciever().build();

        when(adresseregister.getCertificate(RECIEVER_PARTY_NUMBER)).thenThrow(new CertificateException("hello", new VirksertClientException("hello")));

        messageSender.createMessageContext(requestAdapter);
    }

    @Test
    public void shouldThrowMessageContextExceptionWhenMissingSenderCertificate() throws CertificateException, MessageContextException {
        expectedException.expect(MessageContextException.class);
        expectedException.expect(new StatusMatches(StatusMessage.MISSING_SENDER_CERTIFICATE));
        PutMessageRequestWrapper requestAdapter = new RequestBuilder().withSender().withReciever().build();

        when(adresseregister.getCertificate(SENDER_PARTY_NUMBER)).thenThrow(new CertificateException("hello", new VirksertClientException("hello")));

        messageSender.createMessageContext(requestAdapter);
    }

    @Test
    public void ShouldSetConversationId() throws MessageContextException {
        PutMessageRequestWrapper requestAdapter = new RequestBuilder().withSender().withReciever().withConversationId().withJournalpostId().build();

        MessageContext context = messageSender.createMessageContext(requestAdapter);
        Assert.assertEquals(CONVERSATION_ID, context.getConversationId());
    }

    @Test
    public void ShouldSetJournalpostId() throws MessageContextException {
        PutMessageRequestWrapper requestAdapter = new RequestBuilder().withSender().withReciever().withConversationId().withJournalpostId().build();

        MessageContext context = messageSender.createMessageContext(requestAdapter);
        Assert.assertEquals(JOURNALPOST_ID, context.getJournalPostId());
    }

    private class RequestBuilder {
        private PutMessageRequestWrapper requestAdapter;

        private RequestBuilder() {
            this.requestAdapter = mock(PutMessageRequestWrapper.class);
        }

        public RequestBuilder withSender() {
            when(requestAdapter.getSenderPartynumber()).thenReturn(SENDER_PARTY_NUMBER);
            return this;
        }

        public RequestBuilder withReciever() {
            when(requestAdapter.getRecieverPartyNumber()).thenReturn(RECIEVER_PARTY_NUMBER);
            when(requestAdapter.hasRecieverPartyNumber()).thenReturn(true);
            return this;
        }

        public  RequestBuilder withConversationId(){
            when(requestAdapter.getConversationId()).thenReturn(CONVERSATION_ID);
            return this;
        }

        public RequestBuilder withJournalpostId(){
            Object message = "<Melding><journpost><jpId>"+JOURNALPOST_ID+"</jpId></journpost></Melding>";
            when(requestAdapter.getPayload()).thenReturn(message);
            when(requestAdapter.getJournalPostId()).thenReturn(JOURNALPOST_ID);
            return this;
        }

        public PutMessageRequestWrapper build() {
            return requestAdapter;
        }


    }

    private class StatusMatches extends TypeSafeMatcher<MessageContextException> {
        private final StatusMessage expectedStatusMessage;

        public StatusMatches(StatusMessage expectedStatusMessage) {
            this.expectedStatusMessage = expectedStatusMessage;
        }

        @Override
        protected boolean matchesSafely(MessageContextException e) {
            return e.getStatusMessage() == expectedStatusMessage;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("with status ").appendValue(expectedStatusMessage);
        }

        @Override
        public void describeMismatchSafely(MessageContextException exception, Description mismatchDescription) {
            mismatchDescription.appendText("was ").appendValue(exception.getStatusMessage());
        }
    }


}