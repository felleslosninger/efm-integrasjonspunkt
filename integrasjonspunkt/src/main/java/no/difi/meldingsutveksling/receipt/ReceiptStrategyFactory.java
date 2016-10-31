package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.receipt.strategy.DpvReceiptStrategy;
import no.difi.meldingsutveksling.receipt.strategy.EduReceiptStrategy;
import no.difi.meldingsutveksling.receipt.strategy.NoOperationStrategy;

public class ReceiptStrategyFactory {

    private IntegrasjonspunktProperties integrasjonspunktProperties;

    public ReceiptStrategyFactory(IntegrasjonspunktProperties integrasjonspunktProperties) {
        this.integrasjonspunktProperties = integrasjonspunktProperties;
    }

    public ReceiptStrategy getFactory(MessageReceipt receipt) {
        switch (receipt.getTargetType()) {
            case DPV:
                return new DpvReceiptStrategy();
            case EDU:
                return new EduReceiptStrategy();
            default:
                return new NoOperationStrategy();
        }
    }
}
