package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.putmessage.KeystoreProvider;
import no.difi.meldingsutveksling.ptp.MeldingsformidlerClient;

public class DpiReceiptService {
    private IntegrasjonspunktProperties properties;
    private KeystoreProvider keystoreProvider;

    public DpiReceiptService(IntegrasjonspunktProperties properties, KeystoreProvider keystoreProvider) {
        this.properties = properties;
        this.keystoreProvider = keystoreProvider;
    }

    public ExternalReceipt checkForReceipts() {
        MeldingsformidlerClient.Config config = MeldingsformidlerClient.Config.from(properties.getDpi(), keystoreProvider.getKeyStore());
        MeldingsformidlerClient client = new MeldingsformidlerClient(config);
        return client.sjekkEtterKvittering(properties.getOrg().getNumber());
    }
}
