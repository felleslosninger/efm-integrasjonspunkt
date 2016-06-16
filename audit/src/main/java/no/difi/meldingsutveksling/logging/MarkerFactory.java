package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;

public class MarkerFactory {
    private static final String CONVERSATION_ID = "conversation_id";
    private static final String JOURNALPOST_ID = "journalpost_id";
    private static final String RECEIVER_ORG_NUMBER = "receiver_org_number";

    private static final String SENDER_ORG_NUMBER = "sender_org_number";
    private static final String RESPONSE_TYPE = "response-type";
    private static final String RESPONSE_STATUS_MESSAGE_TEXT = "response-message-text";
    private static final String RESPONSE_STATUS_MESSAGE_CODE = "response-message-code";
    private static final String MESSAGE_TYPE = "message-type";

    /**
     * Simple factory pattern
     */
    private MarkerFactory() {
    }

    public static LogstashMarker conversationIdMarker(String conversationId) {
        return Markers.append(CONVERSATION_ID, conversationId);
    }

    public static LogstashMarker journalPostIdMarker(String journalPostId) {
        return Markers.append(JOURNALPOST_ID, journalPostId);
    }

    public static LogstashMarker messageTypeMarker(String messageType) {
        return Markers.append(MESSAGE_TYPE, messageType);
    }

    public static LogstashMarker receiverMarker(String recieverPartyNumber) {
        return Markers.append(RECEIVER_ORG_NUMBER, recieverPartyNumber);
    }

    public static LogstashMarker senderMarker(String senderPartynumber) {
        return Markers.append(SENDER_ORG_NUMBER, senderPartynumber);
    }

    public static LogstashMarker responseTypeMarker(String responseType) {
        return Markers.append(RESPONSE_TYPE, responseType);
    }

    public static LogstashMarker responseMessageCodeMarker(String statusMessageCode) {
        return Markers.append(RESPONSE_STATUS_MESSAGE_CODE, statusMessageCode);
    }

    public static LogstashMarker responseMessageTextMarker(String statusMessageText) {
        return Markers.append(RESPONSE_STATUS_MESSAGE_TEXT, statusMessageText);
    }

}
