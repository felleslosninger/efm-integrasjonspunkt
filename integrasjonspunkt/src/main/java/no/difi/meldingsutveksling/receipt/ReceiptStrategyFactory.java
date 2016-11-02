package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.receipt.strategy.DpvReceiptStrategy;
import no.difi.meldingsutveksling.receipt.strategy.EduReceiptStrategy;
import no.difi.meldingsutveksling.receipt.strategy.NoOperationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReceiptStrategyFactory {

    private DpvReceiptStrategy dpvReceiptStrategy;
    private EduReceiptStrategy eduReceiptStrategy;

    @Autowired
    ReceiptStrategyFactory(DpvReceiptStrategy dpvReceiptStrategy,
                           EduReceiptStrategy eduReceiptStrategy) {
        this.dpvReceiptStrategy = dpvReceiptStrategy;
        this.eduReceiptStrategy = eduReceiptStrategy;
    }

    public ReceiptStrategy getFactory(MessageReceipt receipt) {
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
