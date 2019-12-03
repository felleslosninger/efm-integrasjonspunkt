package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.sdp.client2.domain.kvittering.KvitteringsInfo;
import no.difi.sdp.client2.domain.kvittering.LeveringsKvittering;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_ZONE_ID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ReceiptTypeTest {

    private final Clock clock = Clock.fixed(Instant.parse("2019-03-25T11:38:23Z"), DEFAULT_ZONE_ID);

    private DpiReceiptMapper dpiReceiptMapper;

    @Before
    public void before() {
        this.dpiReceiptMapper = new DpiReceiptMapper(new MessageStatusFactory(clock), clock);
    }

    @Test
    public void receiptTypeOfLeveringsKvitteringShouldBeDelievered() {
        LeveringsKvittering leveringsKvittering = new LeveringsKvittering(null, KvitteringsInfo.builder()
                .tidspunkt(Instant.now(clock))
                .konversasjonsId("123")
                .referanseTilMeldingId("ref123")
                .build());

        MessageStatus actual = dpiReceiptMapper.from(leveringsKvittering);

        assertThat(ReceiptStatus.valueOf(actual.getStatus()), is(ReceiptStatus.LEVERT));
    }
}