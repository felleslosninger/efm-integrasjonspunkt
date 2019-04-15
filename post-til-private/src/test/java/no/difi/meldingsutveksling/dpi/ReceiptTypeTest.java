package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.sdp.client2.domain.kvittering.KvitteringsInfo;
import no.difi.sdp.client2.domain.kvittering.LeveringsKvittering;
import org.junit.Test;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ReceiptTypeTest {
    @Test
    public void receiptTypeOfLeveringsKvitteringShouldBeDelievered() {
        LeveringsKvittering leveringsKvittering = new LeveringsKvittering(null, KvitteringsInfo.builder()
                .tidspunkt(Instant.now())
                .konversasjonsId("123")
                .referanseTilMeldingId("ref123")
                .build());

        MessageStatus actual = DpiReceiptMapper.from(leveringsKvittering);

        assertThat(ReceiptStatus.valueOf(actual.getStatus()), is(ReceiptStatus.LEVERT));
    }
}