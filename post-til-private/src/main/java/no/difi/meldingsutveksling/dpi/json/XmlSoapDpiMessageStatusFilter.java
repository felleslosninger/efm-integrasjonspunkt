package no.difi.meldingsutveksling.dpi.json;

import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.MessageStatus;

public class XmlSoapDpiMessageStatusFilter implements DpiMessageStatusFilter {
    @Override
    public boolean test(MessageStatus messageStatus) {
        ReceiptStatus receiptStatus = ReceiptStatus.valueOf(messageStatus.getStatus());
        return receiptStatus == ReceiptStatus.FEIL;
    }
}
