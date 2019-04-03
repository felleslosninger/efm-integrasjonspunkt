package no.difi.meldingsutveksling.receipt;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;

@RequiredArgsConstructor
public class DpiReceiptService {
    private final IntegrasjonspunktProperties properties;
    private final MeldingsformidlerClient meldingsformidlerClient;

    ExternalReceipt checkForReceipts() {
        return meldingsformidlerClient.sjekkEtterKvittering(properties.getOrg().getNumber());
    }
}
