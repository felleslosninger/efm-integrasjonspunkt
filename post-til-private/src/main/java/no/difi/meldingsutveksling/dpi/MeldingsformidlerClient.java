package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.status.ExternalReceipt;
import reactor.core.publisher.Flux;

public interface MeldingsformidlerClient {
    void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException;

    Flux<ExternalReceipt> sjekkEtterKvitteringer(String orgnr, String mpcId);
}
