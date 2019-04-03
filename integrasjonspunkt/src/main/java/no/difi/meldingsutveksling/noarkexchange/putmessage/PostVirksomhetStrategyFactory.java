package no.difi.meldingsutveksling.noarkexchange.putmessage;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;

@RequiredArgsConstructor(staticName = "newInstance")
public class PostVirksomhetStrategyFactory implements MessageStrategyFactory {

    private final CorrespondenceAgencyMessageFactory correspondenceAgencyMessageFactory;
    private final CorrespondenceAgencyClient client;
    private final NoarkClient noarkClient;
    private final InternalQueue internalQueue;

    @Override
    public MessageStrategy create(Object payload) {
        return new PostVirksomhetMessageStrategy(correspondenceAgencyMessageFactory, client, noarkClient, internalQueue);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPV;
    }
}
