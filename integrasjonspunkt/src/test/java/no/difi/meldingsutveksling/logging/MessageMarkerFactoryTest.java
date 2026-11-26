package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.PersonIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.move.common.IdentifierHasher;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageMarkerFactoryTest {

    private static final String PERSONNR = PersonIdentifier.random().getIdentifier();
    private static final String ORGNR = "0192:910077473";

    @Test
    void markerFrom_personnrAsSender_isNotLoggedInClearText() {
        StandardBusinessDocument sbd = sbd(PERSONNR, ORGNR);

        List<Marker> fields = fields(MessageMarkerFactory.markerFrom(sbd));

        assertThat(fields).filteredOn(SingleFieldAppendingMarker.class::isInstance)
                .extracting(field -> ((SingleFieldAppendingMarker) field).toStringSelf())
                .noneMatch(field -> field.contains(PERSONNR));
        assertThat(fields)
                .contains(Markers.append("sender", IdentifierHasher.hashIfPersonnr(PERSONNR)));
    }

    @Test
    void markerFrom_orgnrAsSender_isLoggedInClearText() {
        StandardBusinessDocument sbd = sbd(ORGNR, PERSONNR);

        assertThat(fields(MessageMarkerFactory.markerFrom(sbd)))
                .contains(Markers.append("sender", ORGNR));
    }

    @Test
    void markerFrom_samePersonnrAsSenderAndReceiver_givesSameValue() {
        StandardBusinessDocument sbd = sbd(PERSONNR, PERSONNR);
        String hashed = IdentifierHasher.hashIfPersonnr(PERSONNR);

        assertThat(fields(MessageMarkerFactory.markerFrom(sbd)))
                .contains(Markers.append("sender", hashed), Markers.append("receiver", hashed));
    }

    private static StandardBusinessDocument sbd(String sender, String receiver) {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setBusinessScope(new BusinessScope()
                                .addScope(new Scope()
                                        .setIdentifier("urn:no:difi:profile:arkivmelding:planByggOgGeodata:ver1.0")
                                        .setInstanceIdentifier("37efbd4c-413d-4e2c-bbc5-257ef4a65a45")
                                        .setType("ConversationId")
                                )
                        )
                        .setDocumentIdentification(new DocumentIdentification()
                                .setInstanceIdentifier("ff88849c-e281-4809-8555-7cd54952b916")
                                .setStandard("urn:no:difi:arkivmelding:xsd::arkivmelding")
                                .setType("arkivmelding")
                                .setTypeVersion("1.0")
                        )
                        .setHeaderVersion("1.0")
                )
                .setSenderIdentifier(PartnerIdentifier.parse(sender))
                .setReceiverIdentifier(PartnerIdentifier.parse(receiver));
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
