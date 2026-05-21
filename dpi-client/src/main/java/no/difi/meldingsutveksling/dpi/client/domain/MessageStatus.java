package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MessageStatus {

    // this class only parses a subset of the full DPI API /messages/out/{id}/statuses response
    // the full response also contain a description / beskrivelse as stings like this :
    //
    //     String status,
    //     String beskrivelse,
    //     String timestamp

    private ReceiptStatus status;
    private OffsetDateTime timestamp;

}
