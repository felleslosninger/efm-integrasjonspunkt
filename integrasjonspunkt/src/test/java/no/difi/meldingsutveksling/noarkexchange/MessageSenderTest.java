package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.services.AdresseregisterVirksert;
import no.difi.meldingsutveksling.services.CertificateException;
import no.difi.virksert.client.VirksertClientException;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.*;
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
    public void messageContextShouldHaveConversationId() throws MessageContextException {
        PutMessageRequestWrapper requestAdapter = new RequestBuilder().withSender().withReciever().withConversationId().withJournalpostId().build();

        MessageContext context = messageSender.createMessageContext(requestAdapter);
        Assert.assertEquals(CONVERSATION_ID, context.getConversationId());
    }

    @Test @Ignore
    public void messageContextShouldHaveJournalPostId() throws MessageContextException {
        PutMessageRequestWrapper requestAdapter = new RequestBuilder().withSender().withReciever().withConversationId().withEduStringMessage(JOURNALPOST_ID).build();

        MessageContext context = messageSender.createMessageContext(requestAdapter);
        Assert.assertEquals(JOURNALPOST_ID, context.getJournalPostId());
    }

    @Test @Ignore
    public void messageContextShouldNotHaveJounalpostIdOnAppReceipt() throws MessageContextException{
        PutMessageRequestWrapper requestAdapter = new RequestBuilder().withSender().withReciever().withConversationId().withAppReceipt().build();

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

        public  RequestBuilder withEduStringMessage(String jpId){
            Object message = String.format("<payload xsi:type=\"xsd:string\" xmlns=\"\">&lt;?xml version=\"1.0\" encoding=\"utf-8\"?&gt;\n" +
                    "&lt;Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"&gt;\n" +
                    "  &lt;journpost xmlns=\"\"&gt;\n" +
                    "    &lt;jpId&gt;%s&lt;/jpId&gt;\n" +
                    "    &lt;jpJaar&gt;2016&lt;/jpJaar&gt;\n" +
                    "    &lt;jpSeknr&gt;179&lt;/jpSeknr&gt;\n" +
                    "    &lt;jpJpostnr&gt;19&lt;/jpJpostnr&gt;\n" +
                    "    &lt;jpJdato&gt;0001-01-01&lt;/jpJdato&gt;\n" +
                    "    &lt;jpNdoktype&gt;U&lt;/jpNdoktype&gt;\n" +
                    "    &lt;jpDokdato&gt;2016-05-11&lt;/jpDokdato&gt;\n" +
                    "    &lt;jpStatus&gt;R&lt;/jpStatus&gt;\n" +
                    "    &lt;jpInnhold&gt;logge melding&lt;/jpInnhold&gt;\n" +
                    "    &lt;jpForfdato /&gt;\n" +
                    "    &lt;jpTgkode&gt;U&lt;/jpTgkode&gt;\n" +
                    "    &lt;jpAgdato /&gt;\n" +
                    "    &lt;jpAntved /&gt;\n" +
                    "    &lt;jpSaar&gt;2015&lt;/jpSaar&gt;\n" +
                    "    &lt;jpSaseknr&gt;944&lt;/jpSaseknr&gt;\n" +
                    "    &lt;jpOffinnhold&gt;logge melding&lt;/jpOffinnhold&gt;\n" +
                    "    &lt;jpTggruppnavn&gt;Alle&lt;/jpTggruppnavn&gt;    \n" +
                    "  &lt;/journpost&gt;</payload>", jpId);
            when(requestAdapter.getPayload()).thenReturn(message);
            return this;
        }

        public  RequestBuilder withEduCDataMessage(){
            Object message = "";
            when(requestAdapter.getPayload()).thenReturn(message);
            return this;
        }

        public RequestBuilder withAppReceipt(){
            Object message = "&lt;AppReceipt type=\"OK\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark/Exchange/types\"&gt;\n" +
                    "  &lt;message code=\"ID\" xmlns=\"\"&gt;\n" +
                    "    &lt;text&gt;210725&lt;/text&gt;\n" +
                    "  &lt;/message&gt;\n" +
                    "&lt;/AppReceipt&gt;";
            when(requestAdapter.getPayload()).thenReturn(message);
            return this;
        }

        public PutMessageRequestWrapper build() {
            return requestAdapter;
        }

        private String getPayloadWithJournalpost(String jpId){
            return String.format("<payload xsi:type=\"xsd:string\" xmlns=\"\">&lt;?xml version=\"1.0\" encoding=\"utf-8\"?&gt;\n" +
                    "&lt;Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"&gt;\n" +
                    "  &lt;journpost xmlns=\"\"&gt;\n" +
                    "    &lt;jpId&gt;%s&lt;/jpId&gt;\n" +
                    "    &lt;jpJaar&gt;2016&lt;/jpJaar&gt;\n" +
                    "    &lt;jpSeknr&gt;179&lt;/jpSeknr&gt;\n" +
                    "    &lt;jpJpostnr&gt;19&lt;/jpJpostnr&gt;\n" +
                    "    &lt;jpJdato&gt;0001-01-01&lt;/jpJdato&gt;\n" +
                    "    &lt;jpNdoktype&gt;U&lt;/jpNdoktype&gt;\n" +
                    "    &lt;jpDokdato&gt;2016-05-11&lt;/jpDokdato&gt;\n" +
                    "    &lt;jpStatus&gt;R&lt;/jpStatus&gt;\n" +
                    "    &lt;jpInnhold&gt;logge melding&lt;/jpInnhold&gt;\n" +
                    "    &lt;jpForfdato /&gt;\n" +
                    "    &lt;jpTgkode&gt;U&lt;/jpTgkode&gt;\n" +
                    "    &lt;jpAgdato /&gt;\n" +
                    "    &lt;jpAntved /&gt;\n" +
                    "    &lt;jpSaar&gt;2015&lt;/jpSaar&gt;\n" +
                    "    &lt;jpSaseknr&gt;944&lt;/jpSaseknr&gt;\n" +
                    "    &lt;jpOffinnhold&gt;logge melding&lt;/jpOffinnhold&gt;\n" +
                    "    &lt;jpTggruppnavn&gt;Alle&lt;/jpTggruppnavn&gt;    \n" +
                    "  &lt;/journpost&gt;</payload>", jpId);
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