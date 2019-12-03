package no.difi.meldingsutveksling.status;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;

import java.util.Optional;

@RequiredArgsConstructor
public class DpiReceiptService {
    private final IntegrasjonspunktProperties properties;
    private final MeldingsformidlerClient meldingsformidlerClient;

    Optional<ExternalReceipt> checkForReceipts() {
        return meldingsformidlerClient.sjekkEtterKvittering(properties.getOrg().getNumber());
    }
}
