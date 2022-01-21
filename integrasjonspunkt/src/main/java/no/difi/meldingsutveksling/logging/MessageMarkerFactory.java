package no.difi.meldingsutveksling.logging;

import lombok.RequiredArgsConstructor;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

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
@Component
@RequiredArgsConstructor
public class MessageMarkerFactory {

    private static final String ALTINN_RECEIPT_ID = "altinn-receipt-id";
    private static final String PAYLOAD_SIZE = "payload-size";

    private final SBDService sbdService;

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
    public LogstashMarker markerFrom(StandardBusinessDocument sbd) {
        LogstashMarker conversationIdMarker = conversationIdMarker(SBDUtil.getConversationId(sbd));
        LogstashMarker messageIdMarker = messageIdMarker(SBDUtil.getMessageId(sbd));
        LogstashMarker messageTypeMarker = MarkerFactory.messageTypeMarker(SBDUtil.getMessageType(sbd).getType());
        LogstashMarker journalPostIdMarker = journalPostIdMarker(SBDUtil.getJournalPostId(sbd));
        LogstashMarker senderMarker = NextMoveMessageMarkers.senderMarker(sbdService.getSender(sbd).asIso6523());
        LogstashMarker senderIdentifierMarker = senderMarker(sbdService.getSenderIdentifier(sbd));
        LogstashMarker receiverMarker = NextMoveMessageMarkers.receiverMarker(sbdService.getReceiver(sbd).asIso6523());
        LogstashMarker receiverIdentifierMarker = receiverMarker(sbdService.getReceiverIdentifier(sbd));
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
