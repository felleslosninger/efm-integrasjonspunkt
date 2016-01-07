package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.isNotEmpty;


public class PutMessageRequestWrapper {
    private PutMessageRequestType requestType;

    public PutMessageRequestWrapper(PutMessageRequestType requestType) {
        this.requestType = requestType;
    }

    public String getSenderPartynumber() {
        if(hasSenderPartyNumber()) {
            return requestType.getEnvelope().getSender().getOrgnr();
        } else {
            return "";
        }
    }

    public String getConversationId() {
        return requestType.getEnvelope().getConversationId();
    }

    public String getRecieverPartyNumber() {
        return requestType.getEnvelope().getReceiver().getOrgnr();
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
}
