package no.difi.meldingsutveksling.ptp;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.receipt.ExternalReceipt;
import no.difi.meldingsutveksling.receipt.MessageReceipt;

public class EmptyKvittering implements ExternalReceipt {

    public static final String EMPTY = "empty";
    public static final MessageReceipt EMPTY_RECEIPT = MessageReceipt.of(EMPTY, EMPTY, EMPTY, EMPTY, ServiceIdentifier.DPI);

    @Override
    public MessageReceipt update(MessageReceipt messageReceipt) {
        if (messageReceipt == null) {
            return EMPTY_RECEIPT;
        } else return messageReceipt;
    }

    @Override
    public void confirmReceipt() {
        /*
         * Do nothing because this is a non-existent/empty receipt where confirmation is undefined.
         */
    }

    @Override
    public String getId() {
        return "empty";
    }

    @Override
    public LogstashMarker logMarkers() {
        return Markers.append("receipt_type", "empty");
    }
}
