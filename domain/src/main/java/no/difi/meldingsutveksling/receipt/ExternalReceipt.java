package no.difi.meldingsutveksling.receipt;

import net.logstash.logback.marker.LogstashMarker;

/**
 * Represents a receipt from an external message provider such as Sikker Digital Post til Innbygger.
 */
public interface ExternalReceipt {
    /**
     * Updates the domain receipt with values from the external receipt
     * @param messageReceipt the domain receipt
     */
    MessageReceipt update(MessageReceipt messageReceipt);

    /**
     * Used if necessary to confirm that the receipt has been successfully received
     */
    void confirmReceipt();

    /**
     * Gets the identificator of the receipt. This should correspond to the conversation id of the original message
     * @return identificator of the receipt/conversation id
     */
    String getId();

    /**
     * Used with logging to identify the receipt
     * @return markers to be used with log statements
     */
    LogstashMarker logMarkers();
}
