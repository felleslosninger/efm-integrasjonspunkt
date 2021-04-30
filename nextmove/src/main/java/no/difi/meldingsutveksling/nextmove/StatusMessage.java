package no.difi.meldingsutveksling.nextmove;

import lombok.*;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@NextMoveBusinessMessage("status")
public class StatusMessage extends BusinessMessage<StatusMessage> {
    private ReceiptStatus status;
}
