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
        var result = mapper.from(sbdWith("aapningskvittering", new Aapningskvittering()));

        assertThat(result.getStatus()).isEqualTo(ReceiptStatus.LEST.toString());
    }

    @Test
    void givenMottakskvittering_thenStatusIsLest() {
        var result = mapper.from(sbdWith("mottakskvittering", new Mottakskvittering()));

        assertThat(result.getStatus()).isEqualTo(ReceiptStatus.LEVERT.toString());
    }

    @Test
    void givenReturpostkvittering_thenStatusIsLest() {
        var result = mapper.from(sbdWith("returpostkvittering", new Returpostkvittering()));

        assertThat(result.getStatus()).isEqualTo(ReceiptStatus.FEIL.toString());
    }

    @Test
    void givenVarslingfeiletkvittering_thenStatusIsFeil() {
        var result = mapper.from(sbdWith("varslingfeiletkvittering", new Varslingfeiletkvittering()));

        assertThat(result.getStatus()).isEqualTo(ReceiptStatus.FEIL.toString());
    }

    private StandardBusinessDocument sbdWith(String type, Object kvittering) {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setDocumentIdentification(new DocumentIdentification()
                                .setType(type)))
                .setAny(kvittering);
    }

}
