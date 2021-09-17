package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.status.ExternalReceipt;

import java.util.Collection;
import java.util.function.Consumer;

public interface MeldingsformidlerClient {
    Collection<String> getPartitionIds();

    boolean shouldValidatePartitionId();

    void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException;

    void sjekkEtterKvitteringer(String partitionId, Consumer<ExternalReceipt> callback);
}
