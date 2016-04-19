package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.isNotEmpty;


public class PutMessageRequestWrapper {
    private PutMessageRequestType requestType;

    public PutMessageRequestWrapper(PutMessageRequestType requestType) {
        this.requestType = requestType;

    }

    public boolean hasNOARKPayload() {
        return requestType.getPayload() != null;
    }

    public String getSenderPartynumber() {
        if (hasSenderPartyNumber()) {
            return requestType.getEnvelope().getSender().getOrgnr();
        } else {
            return "";
        }
    }

    public String getConversationId() {
        return requestType.getEnvelope().getConversationId();
    }

    public String getRecieverPartyNumber() {
        final String originalOrgNr = requestType.getEnvelope().getReceiver().getOrgnr();
        return FiksFix.replaceOrgNummberWithKs(originalOrgNr);
    }

    public boolean hasSenderPartyNumber() {
        return hasSender() && isNotBlank(requestType.getEnvelope().getSender().getOrgnr());
    }

    public boolean hasRecieverPartyNumber() {
        return hasReciever() && isNotEmpty(getRecieverPartyNumber());
    }

    private boolean hasReciever() {
        return hasEnvelope() && requestType.getEnvelope().getReceiver() != null;
    }

    private boolean hasSender() {
        return hasEnvelope() && requestType.getEnvelope().getSender() != null;
    }

    private boolean hasEnvelope() {
        return requestType.getEnvelope() != null;
    }

    public void setSenderPartyNumber(String senderPartyNumber) {
        this.requestType.getEnvelope().getSender().setOrgnr(senderPartyNumber);
    }

    public Object getPayload() {
        return requestType.getPayload();
    }

    public PutMessageRequestType getRequest() {
        return requestType;
    }

    public void setReceiverPartyNumber(String receiverPartyNumber) {
        this.requestType.getEnvelope().getReceiver().setOrgnr(receiverPartyNumber);
    }

    public String getJournalPostId() {
        return JournalpostId.fromPutMessage(this).value();
    }

    public void swapSenderAndReceiver() {
        final String sender = getSenderPartynumber() ;
        setSenderPartyNumber(getRecieverPartyNumber());
        setReceiverPartyNumber(sender);
    }
}
