package no.difi.meldingsutveksling.logging;

import net.logstash.logback.marker.Markers;
import no.difi.move.common.IdentifierHasher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NextMoveMessageMarkersTest {

    private static final String PERSONNR = "01017012345";
    private static final String ORGNR = "910077473";

    @Test
    void senderMarker_hashesPersonnr() {
        assertThat(NextMoveMessageMarkers.senderMarker(PERSONNR))
                .isEqualTo(Markers.append("sender", IdentifierHasher.hashIfPersonnr(PERSONNR)))
                .isNotEqualTo(Markers.append("sender", PERSONNR));
    }

    @Test
    void senderMarker_keepsOrgnrInClearText() {
        assertThat(NextMoveMessageMarkers.senderMarker(ORGNR))
                .isEqualTo(Markers.append("sender", ORGNR));
    }

    @Test
    void senderMarker_handlesNull() {
        assertThat(NextMoveMessageMarkers.senderMarker(null))
                .isEqualTo(Markers.append("sender", null));
    }

    @Test
    void senderAndReceiverMarker_givesSameValueForSamePersonnr() {
        String hashed = IdentifierHasher.hashIfPersonnr(PERSONNR);

        assertThat(NextMoveMessageMarkers.senderMarker(PERSONNR))
                .isEqualTo(Markers.append("sender", hashed));
        assertThat(NextMoveMessageMarkers.receiverMarker(PERSONNR))
                .isEqualTo(Markers.append("receiver", hashed));
    }
}
