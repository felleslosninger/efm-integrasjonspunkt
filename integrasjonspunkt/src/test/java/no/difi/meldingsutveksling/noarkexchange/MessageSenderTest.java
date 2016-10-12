package no.difi.meldingsutveksling.noarkexchange;

import java.security.cert.CertificateException;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.Receiver;
import no.difi.meldingsutveksling.core.Sender;
import no.difi.meldingsutveksling.noarkexchange.schema.core.JournpostType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.services.Adresseregister;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

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
    private Adresseregister adresseregister;

    @Mock
    private IntegrasjonspunktProperties propertiesMock;

    @Mock
    private IntegrasjonspunktNokkel integrasjonspunktNokkel;

    @Before
    public void setUp() {
        messageSender = new MessageSender();
        messageSender.setProperties(propertiesMock);
        messageSender.setKeyInfo(integrasjonspunktNokkel);
        messageSender.setAdresseregister(adresseregister);
    }

    @Test
    public void shouldThrowMessageContextExceptionWhenMissingRecipientOrganizationNumber() throws MessageContextException {
        expectedException.expect(MessageContextException.class);
        expectedException.expect(new StatusMatches(StatusMessage.MISSING_RECIEVER_ORGANIZATION_NUMBER));
        EDUCore request = new RequestBuilder().withSender().build();
        when(request.getReceiver().getOrgNr()).thenReturn("");

        messageSender.createMessageContext(request);
    }

    @Test
    public void shouldThrowMessageContextExceptionWhenMissingRecipientCertificate() throws MessageContextException, CertificateException {
        expectedException.expect(MessageContextException.class);
        expectedException.expect(new StatusMatches(StatusMessage.MISSING_RECIEVER_CERTIFICATE));
        EDUCore request = new RequestBuilder().withSender().withReciever().build();

        when(adresseregister.getCertificate(RECIEVER_PARTY_NUMBER)).thenThrow(new CertificateException("hello"));

        messageSender.createMessageContext(request);
    }

    @Test
    public void shouldThrowMessageContextExceptionWhenMissingSenderCertificate() throws CertificateException, MessageContextException {
        expectedException.expect(MessageContextException.class);
        expectedException.expect(new StatusMatches(StatusMessage.MISSING_SENDER_CERTIFICATE));
        EDUCore request = new RequestBuilder().withSender().withReciever().build();

        when(adresseregister.getCertificate(SENDER_PARTY_NUMBER)).thenThrow(new CertificateException("hello"));

        messageSender.createMessageContext(request);
    }

    @Test
    public void messageContextShouldHaveConversationId() throws MessageContextException {
        EDUCore request = new RequestBuilder().withSender().withReciever().withConversationId().withJournalpostId().build();

        MessageContext context = messageSender.createMessageContext(request);
        Assert.assertEquals(CONVERSATION_ID, context.getConversationId());
    }

    @Test
    public void messageContextShouldHaveJournalPostId() throws MessageContextException {
        EDUCore request = new RequestBuilder().withSender().withReciever().withConversationId().withJournalpostId().build();

        when(request.getMessageType()).thenReturn(EDUCore.MessageType.EDU);
        MessageContext context = messageSender.createMessageContext(request);
        Assert.assertEquals(JOURNALPOST_ID, context.getJournalPostId());
    }

    @Test
    public void messageContextShouldHaveEmptyJounalpostIdOnAppReceipt() throws MessageContextException {
        EDUCore request = new RequestBuilder().withSender().withReciever().withConversationId().build();

        when(request.getMessageType()).thenReturn(EDUCore.MessageType.APPRECEIPT);
        MessageContext context = messageSender.createMessageContext(request);
        Assert.assertEquals("", context.getJournalPostId());
    }

    private class RequestBuilder {

        private EDUCore request;

        private RequestBuilder() {
            this.request = mock(EDUCore.class);
            when(request.getSender()).thenReturn(mock(Sender.class));
            when(request.getReceiver()).thenReturn(mock(Receiver.class));
        }

        public RequestBuilder withSender() {
            when(request.getSender().getOrgNr()).thenReturn(SENDER_PARTY_NUMBER);
            return this;
        }

        public RequestBuilder withReciever() {
            when(request.getReceiver().getOrgNr()).thenReturn(RECIEVER_PARTY_NUMBER);
            return this;
        }

        public RequestBuilder withConversationId() {
            when(request.getId()).thenReturn(CONVERSATION_ID);
            return this;
        }

        public RequestBuilder withJournalpostId() {
            when(request.getPayloadAsMeldingType()).thenReturn(mock(MeldingType.class));
            when(request.getPayloadAsMeldingType().getJournpost()).thenReturn(mock(JournpostType.class));
            when(request.getPayloadAsMeldingType().getJournpost().getJpId()).thenReturn(JOURNALPOST_ID);
            when(request.getJournalpostId()).thenReturn(JOURNALPOST_ID);
            return this;
        }

        public EDUCore build() {
            return request;
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
