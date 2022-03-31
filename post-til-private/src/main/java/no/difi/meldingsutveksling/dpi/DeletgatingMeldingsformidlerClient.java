package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.status.ExternalReceipt;
import no.difi.meldingsutveksling.status.MessageStatus;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DeletgatingMeldingsformidlerClient implements MeldingsformidlerClient {

    private final List<MeldingsformidlerClient> delegates;
    private final MeldingsformidlerClient mainDelegate;

    public DeletgatingMeldingsformidlerClient(List<MeldingsformidlerClient> delegates) {
        this.delegates = delegates;
        this.mainDelegate = delegates.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Expected at least one delegate!"));
    }

    @Override
    public boolean skalPolleMeldingStatus() {
        return delegates.stream().anyMatch(MeldingsformidlerClient::skalPolleMeldingStatus);
    }

    @Override
    public void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException {
        mainDelegate.sendMelding(request);
    }

    @Override
    public void sjekkEtterKvitteringer(String avsenderidentifikator, String mpcId, Consumer<ExternalReceipt> callback) {
        delegates.forEach(delegate -> delegate.sjekkEtterKvitteringer(avsenderidentifikator, mpcId, callback));
    }

    @Override
    public Stream<MessageStatus> hentMeldingStatusListe(String messageId) {
        return delegates.stream().flatMap(delegate -> delegate.hentMeldingStatusListe(messageId));
    }
}
