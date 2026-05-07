package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MessageStatus {

    // response from DPI API /messages/out/{id}/statuses
    //
    //     String status,
    //     String beskrivelse,
    //     String timestamp

    private ReceiptStatus status;
    private OffsetDateTime timestamp;

}
