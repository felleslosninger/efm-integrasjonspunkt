package no.difi.meldingsutveksling.dpi.json;

import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.domain.sbdh.DocumentIdentification;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.*;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JsonDpiReceiptMapperTest {

    // testing the mapping logic with timstamp from kvittering and fallback to now()

    private final Clock clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
    private final JsonDpiReceiptMapper mapper = new JsonDpiReceiptMapper(new MessageStatusMapper(new MessageStatusFactory(clock)));

    @Test
    void givenLeveringskvitteringWithTimestamp_thenLastUpdateIsFromReceipt() {
        OffsetDateTime receiptTimestamp = OffsetDateTime.parse("2026-04-30T12:30:06.344+00:00");
        Leveringskvittering kvittering = new Leveringskvittering();
        kvittering.setTidspunkt(receiptTimestamp);

        var result = mapper.from(sbdWith("leveringskvittering", kvittering));

        assertThat(result.getLastUpdate()).isEqualTo(receiptTimestamp);
    }

    @Test
    void givenKvitteringWithNullTidspunkt_thenLastUpdateFallsBackToNow() {
        var result = mapper.from(sbdWith("leveringskvittering", new Leveringskvittering()));

        assertThat(result.getLastUpdate()).isEqualTo(OffsetDateTime.now(clock));
    }

    @Test
    void givenLeveringskvittering_thenStatusIsLevert() {
        var result = mapper.from(sbdWith("leveringskvittering", new Leveringskvittering()));

        assertThat(result.getStatus()).isEqualTo(ReceiptStatus.LEVERT.toString());
        assertThat(result.getLastUpdate()).isEqualTo(OffsetDateTime.now(clock));
    }

    @Test
    void givenAapningskvittering_thenStatusIsLest() {
        OffsetDateTime receiptTimestamp = OffsetDateTime.parse("2026-04-29T12:30:06.344+00:00");
        Aapningskvittering kvittering = new Aapningskvittering();
        kvittering.setTidspunkt(receiptTimestamp);

        var result = mapper.from(sbdWith("aapningskvittering", kvittering));

        assertThat(result.getStatus()).isEqualTo(ReceiptStatus.LEST.toString());
        assertThat(result.getLastUpdate()).isEqualTo(receiptTimestamp);
    }

    @Test
    void givenMottakskvittering_thenStatusIsLevert() {
        OffsetDateTime receiptTimestamp = OffsetDateTime.parse("2026-04-28T12:30:06.344+00:00");
        Mottakskvittering kvittering = new Mottakskvittering();
        kvittering.setTidspunkt(receiptTimestamp);

        var result = mapper.from(sbdWith("mottakskvittering", kvittering));

        assertThat(result.getStatus()).isEqualTo(ReceiptStatus.LEVERT.toString());
        assertThat(result.getLastUpdate()).isEqualTo(receiptTimestamp);
    }

    @Test
    void givenReturpostkvittering_thenStatusIsFeil() {
        OffsetDateTime receiptTimestamp = OffsetDateTime.parse("2026-04-28T12:30:06.344+00:00");
        Returpostkvittering kvittering = new Returpostkvittering();
        kvittering.setTidspunkt(receiptTimestamp);

        var result = mapper.from(sbdWith("returpostkvittering", kvittering));

        assertThat(result.getStatus()).isEqualTo(ReceiptStatus.FEIL.toString());
        assertThat(result.getLastUpdate()).isEqualTo(receiptTimestamp);
    }

    @Test
    void givenVarslingfeiletkvittering_thenStatusIsFeil() {
        OffsetDateTime receiptTimestamp = OffsetDateTime.parse("2026-04-28T12:30:06.344+00:00");
        Varslingfeiletkvittering kvittering = new Varslingfeiletkvittering();
        kvittering.setTidspunkt(receiptTimestamp);

        var result = mapper.from(sbdWith("varslingfeiletkvittering", kvittering));

        assertThat(result.getStatus()).isEqualTo(ReceiptStatus.FEIL.toString());
        assertThat(result.getLastUpdate()).isEqualTo(receiptTimestamp);
    }

    private StandardBusinessDocument sbdWith(String type, Object kvittering) {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setDocumentIdentification(new DocumentIdentification()
                                .setType(type)))
                .setAny(kvittering);
    }

}
