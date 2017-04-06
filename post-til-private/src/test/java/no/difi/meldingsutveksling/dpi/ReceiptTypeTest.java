package no.difi.meldingsutveksling.dpi;

import no.difi.sdp.client2.domain.kvittering.LeveringsKvittering;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ReceiptTypeTest {
    @Test
    public void receiptTypeOfLeveringsKvitteringShouldBeDelievered() {
        LeveringsKvittering leveringsKvittering = new LeveringsKvittering(null, null);

        DpiReceiptStatus actual = DpiReceiptStatus.from(leveringsKvittering);

        assertThat(actual, is(DpiReceiptStatus.DELIEVERED));
    }
}