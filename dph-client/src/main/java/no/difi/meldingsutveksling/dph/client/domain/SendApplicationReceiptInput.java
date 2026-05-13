package no.difi.meldingsutveksling.dph.client.domain;

import lombok.Data;
import no.difi.meldingsutveksling.nextmove.DialogmeldingKvitteringMessage;

@Data
public class SendApplicationReceiptInput {

    private Integer senderHerId;
    private DialogmeldingKvitteringMessage payload;
}
