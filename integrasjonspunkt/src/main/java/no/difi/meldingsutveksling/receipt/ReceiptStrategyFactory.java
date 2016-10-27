package no.difi.meldingsutveksling.receipt;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.putmessage.KeystoreProvider;
import no.difi.meldingsutveksling.receipt.strategy.DpiReceiptStrategy;
import no.difi.meldingsutveksling.receipt.strategy.DpvReceiptStrategy;
import no.difi.meldingsutveksling.receipt.strategy.EduReceiptStrategy;

import static no.difi.meldingsutveksling.receipt.MessageReceiptMarker.markerFrom;

public class ReceiptStrategyFactory {

    private IntegrasjonspunktProperties integrasjonspunktProperties;
    private KeystoreProvider keystoreProvider;

    public ReceiptStrategyFactory(IntegrasjonspunktProperties integrasjonspunktProperties, KeystoreProvider keystoreProvider) {
        this.integrasjonspunktProperties = integrasjonspunktProperties;
        this.keystoreProvider = keystoreProvider;
    }

    public ReceiptStrategy getFactory(MessageReceipt receipt) {
        switch (receipt.getTargetType()) {
            case DPI:
                return new DpiReceiptStrategy(integrasjonspunktProperties, keystoreProvider);
            case DPV:
                final LogstashMarker markers = markerFrom(receipt);
                return new DpvReceiptStrategy(integrasjonspunktProperties, receipt.getMessageId(), markers);
            case EDU:
                return new EduReceiptStrategy();
            default:
                return null;
        }
    }
}
