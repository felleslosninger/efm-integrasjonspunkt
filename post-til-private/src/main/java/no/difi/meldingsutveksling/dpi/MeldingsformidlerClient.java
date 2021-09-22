package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.status.ExternalReceipt;
import no.difi.meldingsutveksling.status.MessageStatus;

import java.util.function.Consumer;
import java.util.stream.Stream;

public interface MeldingsformidlerClient {
    boolean skalPolleMeldingStatus();

    void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException;

    void sjekkEtterKvitteringer(String avsenderidentifikator, String mpcId, Consumer<ExternalReceipt> callback);

    Stream<MessageStatus> hentMeldingStatusListe(String messageId);
}
