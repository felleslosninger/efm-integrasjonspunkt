package no.difi.meldingsutveksling.receipt.strategy;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.putmessage.KeystoreProvider;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerClient;
import no.difi.meldingsutveksling.receipt.ReceiptStrategy;

public class DpiReceiptStrategy implements ReceiptStrategy {

    private IntegrasjonspunktProperties integrasjonspunktProperties;
    private KeystoreProvider keystoreProvider;

    public DpiReceiptStrategy(IntegrasjonspunktProperties integrasjonspunktProperties, KeystoreProvider keystoreProvider) {
        this.integrasjonspunktProperties = integrasjonspunktProperties;
        this.keystoreProvider = keystoreProvider;
    }

    @Override
    public MeldingsformidlerClient.Kvittering getReceipt() {
        MeldingsformidlerClient.Config config = MeldingsformidlerClient.Config.from(integrasjonspunktProperties.getDpi(), keystoreProvider.getKeyStore());
        MeldingsformidlerClient client = new MeldingsformidlerClient(config);
        return client.sjekkEtterKvittering(integrasjonspunktProperties.getOrg().getNumber());
    }
}
