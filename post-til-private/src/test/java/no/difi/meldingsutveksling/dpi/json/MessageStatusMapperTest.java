package no.difi.meldingsutveksling.dpi.json;

import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.dpi.client.domain.ReceiptStatus;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DpiMessageType;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageStatusMapperTest {

    private final Clock clock = Clock.fixed(Instant.parse("2023-01-15T14:13:12Z"), DateTimeUtil.DEFAULT_ZONE_ID);
    private final MessageStatusMapper mapper = new MessageStatusMapper(new MessageStatusFactory(clock));

    @Test
    void getMessageStatus_unknownDpiMessageType_returnsDefaultStatus() {
        MessageStatus result = mapper.getMessageStatus(DpiMessageType.DIGITAL);

        assertEquals("ANNET", result.getStatus());
        assertEquals("Ukjent kvittering", result.getDescription());
        assertEquals(OffsetDateTime.now(clock), result.getLastUpdate());
    }

    // tests for the other DpiMessageType's are handled in these two other tests
    /// @see JsonDpiReceiptMapperTest
    /// @see JsonDpiReceiptMapperJwtTest

    @Test
    void getDefaultMessageStatus() {
        MessageStatus result = mapper.getDefaultMessageStatus();

        assertEquals("ANNET", result.getStatus());
        assertEquals("Ukjent kvittering", result.getDescription());
        assertEquals(OffsetDateTime.now(clock), result.getLastUpdate());
    }

    @Test
    void getMessageStatus_corner2_opprettet() {
        no.difi.meldingsutveksling.dpi.client.domain.MessageStatus in = new no.difi.meldingsutveksling.dpi.client.domain.MessageStatus();
        in.setStatus(ReceiptStatus.OPPRETTET);

        MessageStatus result = mapper.getMessageStatus(in);

        assertEquals("SENDT", result.getStatus());
        assertEquals("Hjørne 2 har mottatt meldingen", result.getDescription());
        assertEquals(OffsetDateTime.now(clock), result.getLastUpdate());
    }

    @Test
    void getMessageStatus_corner2_sendt() {
        no.difi.meldingsutveksling.dpi.client.domain.MessageStatus in = new no.difi.meldingsutveksling.dpi.client.domain.MessageStatus();
        in.setStatus(ReceiptStatus.SENDT);

        MessageStatus result = mapper.getMessageStatus(in);

        assertEquals("MOTTATT", result.getStatus());
        assertEquals("Hjørne 3 har mottatt meldingen", result.getDescription());
        assertEquals(OffsetDateTime.now(clock), result.getLastUpdate());
    }

    @Test
    void getMessageStatus_corner2_sendt_usesInputTimestamp() {
        OffsetDateTime inputTimestamp = OffsetDateTime.now();
        no.difi.meldingsutveksling.dpi.client.domain.MessageStatus in = new no.difi.meldingsutveksling.dpi.client.domain.MessageStatus();
        in.setStatus(ReceiptStatus.SENDT);
        in.setTimestamp(inputTimestamp);

        MessageStatus result = mapper.getMessageStatus(in);

        assertEquals("MOTTATT", result.getStatus());
        assertEquals("Hjørne 3 har mottatt meldingen", result.getDescription());
        assertEquals(inputTimestamp, result.getLastUpdate());
    }

    @Test
    void getMessageStatus_corner2_feilet() {
        no.difi.meldingsutveksling.dpi.client.domain.MessageStatus in = new no.difi.meldingsutveksling.dpi.client.domain.MessageStatus();
        in.setStatus(ReceiptStatus.FEILET);

        MessageStatus result = mapper.getMessageStatus(in);

        assertEquals("FEIL", result.getStatus());
        assertEquals("Generell melding om at det har skjedd en feil", result.getDescription());
        assertEquals(OffsetDateTime.now(clock), result.getLastUpdate());
    }

}
