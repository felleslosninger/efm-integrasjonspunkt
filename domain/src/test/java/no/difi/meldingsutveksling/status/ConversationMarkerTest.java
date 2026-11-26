package no.difi.meldingsutveksling.status;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.move.common.IdentifierHasher;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMarkerTest {

    private static final String PERSONNR = "01017012345";
    private static final String ORGNR = "910077473";

    @Test
    void markerFrom_personnrAsSender_isNotLoggedInClearText() {
        Conversation conversation = conversation(PERSONNR, ORGNR);

        List<Marker> fields = fields(ConversationMarker.markerFrom(conversation));

        assertThat(fields).filteredOn(SingleFieldAppendingMarker.class::isInstance)
                .extracting(field -> ((SingleFieldAppendingMarker) field).toStringSelf())
                .noneMatch(field -> field.contains(PERSONNR));
        assertThat(fields)
                .contains(Markers.append("sender", IdentifierHasher.hashIfPersonnr(PERSONNR)));
    }

    @Test
    void markerFrom_orgnrAsSender_isLoggedInClearText() {
        Conversation conversation = conversation(ORGNR, PERSONNR);

        assertThat(fields(ConversationMarker.markerFrom(conversation)))
                .contains(Markers.append("sender", ORGNR));
    }

    @Test
    void markerFrom_samePersonnrAsSenderAndReceiver_givesSameValue() {
        Conversation conversation = conversation(PERSONNR, PERSONNR);
        String hashed = IdentifierHasher.hashIfPersonnr(PERSONNR);

        assertThat(fields(ConversationMarker.markerFrom(conversation)))
                .contains(Markers.append("sender", hashed), Markers.append("receiver", hashed));
    }

    private static Conversation conversation(String sender, String receiver) {
        return new Conversation()
                .setConversationId("37efbd4c-413d-4e2c-bbc5-257ef4a65a45")
                .setMessageId("ff88849c-e281-4809-8555-7cd54952b916")
                .setSender(sender)
                .setSenderIdentifier(sender)
                .setReceiver(receiver)
                .setReceiverIdentifier(receiver)
                .setServiceIdentifier(ServiceIdentifier.DPO)
                .setDirection(ConversationDirection.INCOMING);
    }

    private static List<Marker> fields(LogstashMarker marker) {
        List<Marker> fields = new ArrayList<>();
        collect(marker, fields);
        return fields;
    }

    private static void collect(Marker marker, List<Marker> fields) {
        if (marker instanceof SingleFieldAppendingMarker) {
            fields.add(marker);
        }
        for (Iterator<Marker> it = marker.iterator(); it.hasNext(); ) {
            collect(it.next(), fields);
        }
    }
}
