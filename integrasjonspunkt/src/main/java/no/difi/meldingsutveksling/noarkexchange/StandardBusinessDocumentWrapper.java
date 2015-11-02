package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;

public class StandardBusinessDocumentWrapper {

    private final StandardBusinessDocument document;

    public StandardBusinessDocumentWrapper(StandardBusinessDocument standardBusinessDocument) {
        this.document = standardBusinessDocument;
    }

    public String getSenderOrgNumber() {
        return document.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue().split(":")[1];
    }

    public String getReceiverOrgNumber() {
        return document.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue().split(":")[1];
    }

    public String getConversationId() {
        return document.getStandardBusinessDocumentHeader().getBusinessScope().getScope().get(0).getInstanceIdentifier();
    }
}
