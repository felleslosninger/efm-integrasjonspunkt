package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.springframework.util.StringUtils;

public class PutMessageRequestWrapper {
    private PutMessageRequestType requestType;

    public enum MessageType {
        EDUMESSAGE, APPRECEIPT, UNKNOWN
    }

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

    public String getReceiverPartyNumber() {
        return requestType.getEnvelope().getReceiver().getOrgnr();
    }

    public boolean hasSenderPartyNumber() {
        return hasSender() && StringUtils.hasText(requestType.getEnvelope().getSender().getOrgnr());
    }

    public boolean hasRecieverPartyNumber() {
        return hasReciever() && StringUtils.hasLength(getReceiverPartyNumber());
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

    public EnvelopeType getEnvelope() {
        return requestType.getEnvelope();
    }

    public PutMessageRequestType getRequest() {
        return requestType;
    }

    public void setReceiverPartyNumber(String receiverPartyNumber) {
        this.requestType.getEnvelope().getReceiver().setOrgnr(receiverPartyNumber);
    }

    public String getJournalPostId() throws PayloadException {
        return JournalpostId.fromPutMessage(this).value();
    }

    public void swapSenderAndReceiver() {
        final String sender = getSenderPartynumber();
        setSenderPartyNumber(getReceiverPartyNumber());
        setReceiverPartyNumber(sender);
    }

    public boolean hasPayload() {
        return !PayloadUtil.isEmpty(getPayload());
    }

    public MessageType getMessageType() {
        if (hasPayload()) {
            if (PayloadUtil.isAppReceipt(getPayload())) {
                return MessageType.APPRECEIPT;
            }
            return MessageType.EDUMESSAGE;
        }
        return MessageType.UNKNOWN;
    }
}
