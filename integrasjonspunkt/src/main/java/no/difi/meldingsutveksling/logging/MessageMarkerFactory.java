package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.apache.commons.io.FileUtils;

import static no.difi.meldingsutveksling.logging.MarkerFactory.*;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.documentTypeMarker;
import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.processMarker;

/**
 * Example usage: import static
 * no.difi.meldingsutveksling.logging.ConversationIdMarkerFactory.markerFrom;
 * <p/>
 * ...
 * <p/>
 * log.error(markerFrom(message), "putting message");
 */
public class MessageMarkerFactory {

    private static final String ALTINN_RECEIPT_ID = "altinn-receipt-id";
    private static final String PAYLOAD_SIZE = "payload-size";

    private MessageMarkerFactory() {
    }

    /**
     * Marker used to log payload size of the message.
     *
     * @param bytes of the payload
     * @return marker containing the size of the payload
     */
    public static LogstashMarker payloadSizeMarker(byte[] bytes) {
        return Markers.append(PAYLOAD_SIZE, FileUtils.byteCountToDisplaySize(bytes.length));
    }

    /**
     * Creates LogstashMarker with conversation id from the
     * StandardBusinessDocument that will appear in the logs when used.
     *
     * @param sbd StandardBusinessDocument
     * @return LogstashMarker
     */
    public static LogstashMarker markerFrom(StandardBusinessDocument sbd) {
        LogstashMarker conversationIdMarker = conversationIdMarker(SBDUtil.getConversationId(sbd));
        LogstashMarker messageIdMarker = messageIdMarker(SBDUtil.getMessageId(sbd));
        LogstashMarker messageTypeMarker = MarkerFactory.messageTypeMarker(SBDUtil.getMessageType(sbd).getType());
        LogstashMarker journalPostIdMarker = journalPostIdMarker(SBDUtil.getJournalPostId(sbd));
        LogstashMarker senderMarker = NextMoveMessageMarkers.senderMarker(sbd.getSenderIdentifier().getIdentifier());
        LogstashMarker senderIdentifierMarker = senderMarker(sbd.getSenderIdentifier().getPrimaryIdentifier());
        LogstashMarker receiverMarker = NextMoveMessageMarkers.receiverMarker(sbd.getReceiverIdentifier().getIdentifier());
        LogstashMarker receiverIdentifierMarker = receiverMarker(sbd.getReceiverIdentifier().getPrimaryIdentifier());
        LogstashMarker documentTypeMarker = documentTypeMarker(SBDUtil.getDocumentType(sbd));
        LogstashMarker processMarker = processMarker(SBDUtil.getProcess(sbd));
        return conversationIdMarker.and(messageTypeMarker)
                .and(messageIdMarker)
                .and(journalPostIdMarker)
                .and(senderMarker)
                .and(senderIdentifierMarker)
                .and(receiverMarker)
                .and(receiverIdentifierMarker)
                .and(documentTypeMarker)
                .and(processMarker);
    }

    public static LogstashMarker markerFrom(FileReference reference) {
        LogstashMarker receiptIdMarker = Markers.append(ALTINN_RECEIPT_ID, reference.getReceiptID());
        LogstashMarker referenceValueMarker = Markers.append("altinn-reference-value", reference.getValue());
        return receiptIdMarker.and(referenceValueMarker);
    }

}
