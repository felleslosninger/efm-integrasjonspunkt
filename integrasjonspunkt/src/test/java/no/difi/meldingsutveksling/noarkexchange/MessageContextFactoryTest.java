package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ServiceRecordObjectMother;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.Receiver;
import no.difi.meldingsutveksling.core.Sender;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
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
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Optional;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageContextFactoryTest {

    private static final String SENDER_PARTY_NUMBER = "910075918";
    private static final String RECIEVER_PARTY_NUMBER = "910077473";
    private static final String CONVERSATION_ID = "conversationId";
    private static final String JOURNALPOST_ID = "journalpostid";
    private static final ServiceIdentifier SERVICE_IDENTIFIER = DPV;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock(lenient = true)
    private Adresseregister adresseregister;
    @Mock
    private IntegrasjonspunktProperties propertiesMock;
    @Mock(lenient = true)
    private ServiceRegistryLookup serviceRegistryLookup;

    private MessageContextFactory messageContextFactory;

    @Before
    public void setUp() {
        IntegrasjonspunktProperties.Organization org = new IntegrasjonspunktProperties.Organization();
        org.setNumber(SENDER_PARTY_NUMBER);
        when(propertiesMock.getOrg()).thenReturn(org);

        messageContextFactory = new MessageContextFactory(propertiesMock, adresseregister, serviceRegistryLookup);

        when(serviceRegistryLookup.getServiceRecord(SENDER_PARTY_NUMBER, DPV))
                .thenReturn(Optional.of(ServiceRecordObjectMother.createDPVServiceRecord(SENDER_PARTY_NUMBER)));
        when(serviceRegistryLookup.getServiceRecord(RECIEVER_PARTY_NUMBER, DPV))
                .thenReturn(Optional.of(ServiceRecordObjectMother.createDPVServiceRecord(RECIEVER_PARTY_NUMBER)));
    }

    @Test
    public void shouldThrowMessageContextExceptionWhenMissingRecipientOrganizationNumber() throws MessageContextException {
        expectedException.expect(MessageContextException.class);
        expectedException.expect(new StatusMatches(StatusMessage.MISSING_RECIEVER_ORGANIZATION_NUMBER));
        EDUCore request = new RequestBuilder().withSender().build();
        when(request.getReceiver().getIdentifier()).thenReturn("");

        messageContextFactory.from(request);
    }

    @Test
    public void shouldThrowMessageContextExceptionWhenMissingRecipientCertificate() throws MessageContextException, CertificateException {
        expectedException.expect(MessageContextException.class);
        expectedException.expect(new StatusMatches(StatusMessage.MISSING_RECIEVER_CERTIFICATE));
        EDUCore request = new RequestBuilder().withSender().withReciever().withServiceIdentifier().build();

        when(adresseregister.getCertificate(ServiceRecordObjectMother.createDPVServiceRecord(SENDER_PARTY_NUMBER)))
                .thenReturn(Mockito.mock(Certificate.class));
        when(adresseregister.getCertificate(ServiceRecordObjectMother.createDPVServiceRecord(RECIEVER_PARTY_NUMBER)))
                .thenThrow(new CertificateException("hello"));

        messageContextFactory.from(request);
    }

    @Test
    public void messageContextShouldHaveConversationId() throws MessageContextException {
        EDUCore request = new RequestBuilder().withSender().withReciever().withConversationId().withJournalpostId().withServiceIdentifier().build();

        MessageContext context = messageContextFactory.from(request);
        Assert.assertEquals(CONVERSATION_ID, context.getConversationId());
    }

    @Test
    public void messageContextShouldHaveJournalPostId() throws MessageContextException {
        EDUCore request = new RequestBuilder().withSender().withReciever().withConversationId().withJournalpostId().withServiceIdentifier().build();

        when(request.getMessageType()).thenReturn(EDUCore.MessageType.EDU);
        MessageContext context = messageContextFactory.from(request);
        Assert.assertEquals(JOURNALPOST_ID, context.getJournalPostId());
    }

    @Test
    public void messageContextShouldHaveEmptyJounalpostIdOnAppReceipt() throws MessageContextException {
        EDUCore request = new RequestBuilder().withSender().withReciever().withConversationId().withServiceIdentifier().build();

        when(request.getMessageType()).thenReturn(EDUCore.MessageType.APPRECEIPT);
        MessageContext context = messageContextFactory.from(request);
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
            when(request.getSender().getIdentifier()).thenReturn(SENDER_PARTY_NUMBER);
            return this;
        }

        public RequestBuilder withReciever() {
            when(request.getReceiver().getIdentifier()).thenReturn(RECIEVER_PARTY_NUMBER);
            return this;
        }

        public RequestBuilder withConversationId() {
            when(request.getId()).thenReturn(CONVERSATION_ID);
            return this;
        }

        public RequestBuilder withJournalpostId() {
            when(request.getJournalpostId()).thenReturn(JOURNALPOST_ID);
            return this;
        }

        public RequestBuilder withServiceIdentifier() {
            when(request.getServiceIdentifier()).thenReturn(SERVICE_IDENTIFIER);
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
