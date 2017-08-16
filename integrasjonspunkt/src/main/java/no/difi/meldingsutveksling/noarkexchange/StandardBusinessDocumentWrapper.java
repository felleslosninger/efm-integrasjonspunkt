package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.Scope;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.List;

/**
 * Wrapper class for StandardBusinessDocument to simplify the interface and hide implementation details
 */
public class StandardBusinessDocumentWrapper {

    private final StandardBusinessDocument document;

    public StandardBusinessDocumentWrapper(StandardBusinessDocument standardBusinessDocument) {
        this.document = standardBusinessDocument;
    }

    public StandardBusinessDocument getDocument() {
        return document;
    }

    public String getSenderOrgNumber() {
        return document.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue().split(":")[1];
    }

    public String getReceiverOrgNumber() {
        return document.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue().split(":")[1];
    }

    public String getDocumentId() {
        return document.getStandardBusinessDocumentHeader().getDocumentIdentification().getInstanceIdentifier();
    }

    public String getConversationId() {
        return findScope(ScopeType.CONVERSATION_ID.name()).getInstanceIdentifier();
    }

    public final String getJournalPostId() {
        return findScope(ScopeType.JOURNALPOST_ID.name()).getInstanceIdentifier();
    }

    public MessageInfo getMessageInfo() {
        return new MessageInfo(getReceiverOrgNumber(), getSenderOrgNumber(), getJournalPostId(), getConversationId(), getMessageType());
    }

    private Scope findScope(String scopeType) {
        final List<Scope> scopes = document.getStandardBusinessDocumentHeader().getBusinessScope().getScope();
        for (Scope scope : scopes) {
            if (scopeType.equals(scope.getType())) {
                return scope;
            }
        }
        return new Scope();
    }

    public boolean isReceipt() {
        return document.getStandardBusinessDocumentHeader().getDocumentIdentification().getType().equalsIgnoreCase(StandardBusinessDocumentHeader.KVITTERING_TYPE);
    }

    public boolean isNextMove() {
        return StandardBusinessDocumentHeader.NEXTMOVE_TYPE.equalsIgnoreCase(document.getStandardBusinessDocumentHeader().getDocumentIdentification().getType());
    }

    public Payload getPayload() {
        if (document.getAny() instanceof Payload) {
            return (Payload) document.getAny();
        } else if (document.getAny() instanceof Node) {
            return unmarshallAnyElement(document.getAny());
        } else {
            throw new MeldingsUtvekslingRuntimeException("Could not cast any element " + document.getAny() + " from " + StandardBusinessDocument.class + " to " + Payload.class);
        }
    }

    private Payload unmarshallAnyElement(Object any) {
        JAXBContext jaxbContextP;
        Unmarshaller unMarshallerP;
        Payload payload;
        try {
            jaxbContextP = JAXBContext.newInstance(Payload.class);
            unMarshallerP = jaxbContextP.createUnmarshaller();
            payload = unMarshallerP.unmarshal((org.w3c.dom.Node) any, Payload.class).getValue();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return payload;
    }

    public String getMessageType() {
        return document.getStandardBusinessDocumentHeader().getDocumentIdentification().getType();
    }

}
