package no.difi.meldingsutveksling.ptp;

import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.sdp.client2.domain.kvittering.ForretningsKvittering;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KvitteringTest {

    private ForretningsKvittering forretningsKvittering;

    @Before
    public void setup() {
        forretningsKvittering = mock(ForretningsKvittering.class);
        when(forretningsKvittering.getMeldingsId()).thenReturn("123");
        when(forretningsKvittering.getTidspunkt()).thenReturn(Instant.now());
    }

    @Test
    public void updateMessageReceiptAsNullOnDpiKvitteringReturnsNewMessageReceipt() {
        MeldingsformidlerClient.Kvittering dpiKvittering = new MeldingsformidlerClient.Kvittering(forretningsKvittering);

        final MessageReceipt result = dpiKvittering.update(null);

        assertThat(result.getMessageId(), is(dpiKvittering.getId()));
    }

}