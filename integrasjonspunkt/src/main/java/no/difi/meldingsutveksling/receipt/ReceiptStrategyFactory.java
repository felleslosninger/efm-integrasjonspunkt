package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.receipt.strategy.DpiReceiptStrategy;
import no.difi.meldingsutveksling.receipt.strategy.DpvReceiptStrategy;
import no.difi.meldingsutveksling.receipt.strategy.EduReceiptStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReceiptStrategyFactory {

    private static DpiReceiptStrategy dpiReceiptStrategy;
    private static DpvReceiptStrategy dpvReceiptStrategy;
    private static EduReceiptStrategy eduReceiptStrategy;

    @Autowired
    ReceiptStrategyFactory(DpiReceiptStrategy dpiReceiptStrategy,
                           DpvReceiptStrategy dpvReceiptStrategy,
                           EduReceiptStrategy eduReceiptStrategy) {
        this.dpiReceiptStrategy = dpiReceiptStrategy;
        this.dpvReceiptStrategy = dpvReceiptStrategy;
        this.eduReceiptStrategy = eduReceiptStrategy;
    }

    public static ReceiptStrategy getFactory(MessageReceipt receipt) {
        switch (receipt.getTargetType()) {
            case DPI:
                return dpiReceiptStrategy;
            case DPV:
                return dpvReceiptStrategy;
            case EDU:
                return eduReceiptStrategy;
            default:
                return null;
        }
    }
}
