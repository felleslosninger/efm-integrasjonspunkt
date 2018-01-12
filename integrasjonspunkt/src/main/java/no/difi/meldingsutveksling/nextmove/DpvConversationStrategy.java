package no.difi.meldingsutveksling.nextmove;

import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.putmessage.PostVirksomhetStrategyFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DpvConversationStrategy implements ConversationStrategy {

    private IntegrasjonspunktProperties props;
    private ServiceRegistryLookup sr;

    @Autowired
    DpvConversationStrategy(IntegrasjonspunktProperties props,
                            ServiceRegistryLookup sr) {
        this.props = props;
        this.sr = sr;
    }

    @Override
    public void send(ConversationResource conversationResource) throws NextMoveException {
        DpvConversationResource cr = (DpvConversationResource) conversationResource;

        PostVirksomhetStrategyFactory dpvFactory = PostVirksomhetStrategyFactory.newInstance(props, null, sr);
        CorrespondenceAgencyConfiguration config = dpvFactory.getConfig();
        InsertCorrespondenceV2 message;
        message = CorrespondenceAgencyMessageFactory.create(config, cr);

        CorrespondenceAgencyClient client = new CorrespondenceAgencyClient(ConversationResourceMarkers.markerFrom(cr), config);
        final CorrespondenceRequest request = new CorrespondenceRequest.Builder()
                .withUsername(config.getSystemUserCode())
                .withPassword(config.getPassword())
                .withPayload(message).build();

        if (client.sendCorrespondence(request) == null) {
            throw new NextMoveException("Failed to create Correspondence Agency Request");
        }

    }

}
