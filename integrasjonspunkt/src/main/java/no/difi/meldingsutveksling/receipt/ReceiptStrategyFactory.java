package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.receipt.strategy.DpvReceiptStrategy;
import no.difi.meldingsutveksling.receipt.strategy.EduReceiptStrategy;
import no.difi.meldingsutveksling.receipt.strategy.NoOperationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReceiptStrategyFactory {

    private static DpvReceiptStrategy dpvReceiptStrategy;
    private static EduReceiptStrategy eduReceiptStrategy;

    @Autowired
    ReceiptStrategyFactory(DpvReceiptStrategy dpvReceiptStrategy,
                           EduReceiptStrategy eduReceiptStrategy) {
        ReceiptStrategyFactory.dpvReceiptStrategy = dpvReceiptStrategy;
        ReceiptStrategyFactory.eduReceiptStrategy = eduReceiptStrategy;
    }

    public static ReceiptStrategy getFactory(MessageReceipt receipt) {
        switch (receipt.getTargetType()) {
            case DPV:
                return dpvReceiptStrategy;
            case EDU:
                return eduReceiptStrategy;
            default:
                return new NoOperationStrategy();
        }
    }
}
