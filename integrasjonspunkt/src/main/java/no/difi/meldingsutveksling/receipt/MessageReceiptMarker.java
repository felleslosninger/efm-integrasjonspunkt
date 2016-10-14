package no.difi.meldingsutveksling.receipt;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;

/**
 * Creates markers for MessageReceipts.
 */
public class MessageReceiptMarker {
    private static final String MESSAGE_ID = "message_id";
    private static final String COMPLETED = "completed";

    public static LogstashMarker markerFrom(MessageReceipt receipt) {
        final LogstashMarker messageIdMarker = Markers.append(MESSAGE_ID, receipt.getMessageId());
        final LogstashMarker completedMarker = Markers.append(COMPLETED, receipt.isReceived());
        return messageIdMarker.and(completedMarker);
    }

}
