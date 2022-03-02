package no.difi.meldingsutveksling.dpi.client;

import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Kvittering;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.DpiMessageType;

public interface ReceiptFactory {

    DpiMessageType getMessageType();

    Kvittering getReceipt(ReceiptInput input);
}
