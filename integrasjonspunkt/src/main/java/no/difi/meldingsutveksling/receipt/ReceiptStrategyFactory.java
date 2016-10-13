package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.receipt.strategy.DpiReceiptStrategy;
import no.difi.meldingsutveksling.receipt.strategy.DpvReceiptStrategy;
import no.difi.meldingsutveksling.receipt.strategy.EduReceiptStrategy;

public class ReceiptStrategyFactory {

    public static ReceiptStrategy getFactory(MessageReceipt receipt) {
        switch (receipt.getTargetType()) {
            case DPI:
                return new DpiReceiptStrategy();
            case DPV:
                return new DpvReceiptStrategy();
            case EDU:
                return new EduReceiptStrategy();
            default:
                return null;
        }
    }
}
