package no.difi.meldingsutveksling.core;

import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;

public class EDUAppReceipt extends EDUCore {

    EDUAppReceipt() {
        super();
        setMessageType(MessageType.APPRECEIPT);
    }

    @Override
    public AppReceiptType getPayload() {
        return (AppReceiptType) super.getPayload();
    }
}
