package no.difi.meldingsutveksling.status;

import net.logstash.logback.marker.LogstashMarker;

/**
 * Represents a receipt from an external message provider such as Sikker Digital Post til Innbygger.
 */
public interface ExternalReceipt {
    /**
     * Used if necessary to confirm that the receipt has been successfully received
     */
    void confirmReceipt();

    /**
     * Gets the identificator of the receipt. This should correspond to the conversation id of the original message
     * @return identificator of the receipt/conversation id
     */
    String getConversationId();

    /**
     * Used with logging to identify the receipt
     * @return markers to be used with log statements
     */
    LogstashMarker logMarkers();

    /**
     * Creates a internal message receipt from this external receipt
     * @return a new domain MessageStatus representing the external receipt
     */
    MessageStatus toMessageStatus();

}
