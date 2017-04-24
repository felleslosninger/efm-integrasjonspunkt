package no.difi.meldingsutveksling.dpi;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ExternalReceipt;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;

import java.time.LocalDateTime;

public class EmptyKvittering implements ExternalReceipt {

    public static final String EMPTY = "empty";
    private static final MessageStatus EMPTY_RECEIPT = MessageStatus.of(ReceiptStatus.OTHER, LocalDateTime.now());

    @Override
    public void confirmReceipt() {
        /*
         * Do nothing because this is a non-existent/empty receipt where confirmation is undefined.
         */
    }

    @Override
    public String getId() {
        return EMPTY;
    }

    @Override
    public LogstashMarker logMarkers() {
        return Markers.append("receipt_type", EMPTY);
    }

    @Override
    public MessageStatus toMessageStatus() {
        return EMPTY_RECEIPT;
    }

    @Override
    public void auditLog() {
        Audit.info("Got empty receipt", logMarkers());
    }

    @Override
    public Conversation createConversation() {
        return Conversation.of("", "", "", "empty receipt", null, EMPTY_RECEIPT);
    }
}
