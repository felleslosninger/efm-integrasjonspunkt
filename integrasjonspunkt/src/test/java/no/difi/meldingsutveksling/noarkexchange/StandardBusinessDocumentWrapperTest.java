package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.dokumentpakking.service.ScopeFactory;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.BusinessScope;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.DocumentIdentification;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.Scope;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocumentHeader;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class StandardBusinessDocumentWrapperTest {

    public static final String DEFAULT_IDENTIFIER = "urn:no:difi:sdp:1.0";
    public static final String TYPE_CONVERSATION_ID = ScopeType.ConversationId.name();
    public static final String TYPE_JOURNAL_POST_ID = ScopeType.JournalpostId.name();

    public static final String JOURNALPOST_ID = "1234";
    public static final String CONVERSATION_ID = "5555";
    public static final String DOCUMENT_INSTANCE_IDENTIFIER = "9999";
    private StandardBusinessDocument standardBusinessDocument;

    @Before
    public void setup() {
        standardBusinessDocument = new StandardBusinessDocument();
        standardBusinessDocument.setStandardBusinessDocumentHeader(createHeader());
    }

    private StandardBusinessDocumentHeader createHeader() {
        final StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();
        final Scope journalPostIdScope = createScope(JOURNALPOST_ID, TYPE_JOURNAL_POST_ID);
        final Scope conversationIdScope = createScope(CONVERSATION_ID, TYPE_CONVERSATION_ID);
        final DocumentIdentification documentIdentification = new DocumentIdentification();
        documentIdentification.setInstanceIdentifier(DOCUMENT_INSTANCE_IDENTIFIER);
        header.setDocumentIdentification(documentIdentification);
        header.setBusinessScope(createBusinessScope(journalPostIdScope, conversationIdScope));
        return header;
    }

    private BusinessScope createBusinessScope(Scope journalPostIdScope, Scope conversationIdScope) {
        BusinessScope businessScope = new BusinessScope();
        businessScope.getScope().addAll(Arrays.asList(journalPostIdScope, conversationIdScope));
        return businessScope;
    }

    private Scope createScope(String scopeId, String typeConversationId) {
        final Scope scope = new Scope();
        scope.setInstanceIdentifier(scopeId);
        scope.setType(typeConversationId);
        scope.setIdentifier(DEFAULT_IDENTIFIER);
        return scope;
    }

    @Test
    public void getDocumentIdReturnsDocumentInstanceIdentifier() throws Exception {
        StandardBusinessDocumentWrapper wrapper = new StandardBusinessDocumentWrapper(standardBusinessDocument);

        final String actual = wrapper.getDocumentId();

        assertEquals(DOCUMENT_INSTANCE_IDENTIFIER, actual);
    }

    @Test
    public void getConversationIdReturnsConversationId() throws Exception {
        StandardBusinessDocumentWrapper wrapper = new StandardBusinessDocumentWrapper(standardBusinessDocument);

        final String actual = wrapper.getConversationId();

        assertEquals(CONVERSATION_ID, actual);
    }

    @Test
    public void getJournalPostIdReturnsJournalPostId() {
        StandardBusinessDocumentWrapper wrapper = new StandardBusinessDocumentWrapper(standardBusinessDocument);

        final String actual = wrapper.getJournalPostId();

        assertEquals(JOURNALPOST_ID, actual);
    }
}