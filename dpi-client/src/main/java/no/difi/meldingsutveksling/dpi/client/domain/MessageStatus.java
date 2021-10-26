package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MessageStatus {

    private no.difi.meldingsutveksling.dpi.client.domain.ReceiptStatus status;
    private OffsetDateTime timestamp;
}
