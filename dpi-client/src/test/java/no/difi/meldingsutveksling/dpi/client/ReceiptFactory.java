package no.difi.meldingsutveksling.dpi.client;

import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Kvittering;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.MessageType;

public interface ReceiptFactory {

    MessageType getMessageType();

    Kvittering getReceipt(ReceiptInput input);
}
