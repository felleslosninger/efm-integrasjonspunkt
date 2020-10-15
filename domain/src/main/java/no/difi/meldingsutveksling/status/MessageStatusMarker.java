package no.difi.meldingsutveksling.status;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.logging.MarkerFactory;

public class MessageStatusMarker {

    private MessageStatusMarker() {
    }

    public static LogstashMarker from(MessageStatus ms) {
        LogstashMarker statusMarker = Markers.append("status", ms.getStatus());
        LogstashMarker conversationIdMarker = MarkerFactory.conversationIdMarker(ms.getConversationId());
        LogstashMarker messageIdMarker = MarkerFactory.messageIdMarker(ms.getMessageId());
        LogstashMarker descriptionMarker = Markers.append("description", ms.getDescription());
        LogstashMarker rawReceiptMarker = Markers.append("raw_receipt", ms.getRawReceipt());
        return statusMarker.and(conversationIdMarker).and(messageIdMarker).and(descriptionMarker).and(rawReceiptMarker);
    }


}
