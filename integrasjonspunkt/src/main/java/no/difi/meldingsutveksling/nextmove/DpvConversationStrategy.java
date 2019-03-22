package no.difi.meldingsutveksling.nextmove;

import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.noarkexchange.putmessage.PostVirksomhetStrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class DpvConversationStrategy implements ConversationStrategy {

    private final IntegrasjonspunktProperties props;
    private final ServiceRegistryLookup sr;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final InternalQueue internalQueue;

    @Autowired
    DpvConversationStrategy(IntegrasjonspunktProperties props,
                            ServiceRegistryLookup sr,
                            CryptoMessagePersister cryptoMessagePersister,
                            @Lazy InternalQueue internalQueue) {
        this.props = props;
        this.sr = sr;
        this.cryptoMessagePersister = cryptoMessagePersister;
        this.internalQueue = internalQueue;
    }

    @Override
    public void send(ConversationResource conversationResource) throws NextMoveException {
        throw new UnsupportedOperationException("ConversationResource no longer in use");
    }

    @Override
    public void send(NextMoveMessage message) throws NextMoveException {

        PostVirksomhetStrategyFactory dpvFactory = PostVirksomhetStrategyFactory.newInstance(props, null, sr, internalQueue);
        CorrespondenceAgencyConfiguration config = dpvFactory.getConfig();
        InsertCorrespondenceV2 insertCorrespondenceV2 = CorrespondenceAgencyMessageFactory.create(config, message, cryptoMessagePersister);

        CorrespondenceAgencyClient client = new CorrespondenceAgencyClient(NextMoveMessageMarkers.markerFrom(message), config);
        final CorrespondenceRequest request = new CorrespondenceRequest.Builder()
                .withUsername(config.getSystemUserCode())
                .withPassword(config.getPassword())
                .withPayload(insertCorrespondenceV2).build();

        if (client.sendCorrespondence(request) == null) {
            throw new NextMoveException("Failed to create Correspondence Agency Request");
        }
    }

}
