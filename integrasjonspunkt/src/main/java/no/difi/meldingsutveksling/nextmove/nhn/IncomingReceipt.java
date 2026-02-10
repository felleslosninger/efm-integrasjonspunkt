package no.difi.meldingsutveksling.nextmove.nhn;

import java.util.List;

public record IncomingReceipt(
    String receiverHerId,
    StatusForMottakAvMelding status,
    List<ApplicationReceiptError> errors
) {
}

