package no.difi.meldingsutveksling.status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DpiReceiptHandler;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;

@Slf4j
@RequiredArgsConstructor
public class DpiReceiptService {

    private final MeldingsformidlerClient meldingsformidlerClient;
    private final DpiReceiptHandler dpiReceiptHandler;
    private final AvsenderidentifikatorHolder avsenderidentifikatorHolder;

    public void handleReceipts(String mpcId) {
        if (avsenderidentifikatorHolder.pollWithoutAvsenderidentifikator()) {
            meldingsformidlerClient.sjekkEtterKvitteringer(null, mpcId, dpiReceiptHandler::handleReceipt);
        }

        avsenderidentifikatorHolder.getAvsenderidentifikatorListe().forEach(avsenderidentifikator ->
                meldingsformidlerClient.sjekkEtterKvitteringer(avsenderidentifikator, mpcId, dpiReceiptHandler::handleReceipt));
    }

}
