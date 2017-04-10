package no.difi.meldingsutveksling.dpi;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ExternalReceipt;
import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;

import java.time.LocalDateTime;

public class EmptyKvittering implements ExternalReceipt {

    public static final String EMPTY = "empty";
    private static final MessageReceipt EMPTY_RECEIPT = MessageReceipt.of(GenericReceiptStatus.OTHER.toString(), LocalDateTime
            .now());

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
    public MessageReceipt toMessageReceipt() {
        return EMPTY_RECEIPT;
    }

    @Override
    public void auditLog() {
        Audit.info("Got empty receipt", logMarkers());
    }

    @Override
    public Conversation createConversation() {
        return Conversation.of("", "", "", "empty receipt", null);
    }
}
