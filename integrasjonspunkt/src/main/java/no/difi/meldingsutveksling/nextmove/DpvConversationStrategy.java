package no.difi.meldingsutveksling.nextmove;

import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.logging.ConversationResourceMarkers;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.putmessage.PostVirksomhetStrategyFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;
import no.difi.meldingsutveksling.receipt.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DpvConversationStrategy implements ConversationStrategy {

    private IntegrasjonspunktProperties props;
    private MessagePersister messagePersister;
    private ConversationService conversationService;

    @Autowired
    DpvConversationStrategy(IntegrasjonspunktProperties props,
                            MessagePersister messagePersister,
                            ConversationService conversationService) {
        this.props = props;
        this.messagePersister = messagePersister;
        this.conversationService = conversationService;
    }

    @Override
    public void send(ConversationResource conversationResource) throws NextMoveException {
        DpvConversationResource cr = (DpvConversationResource) conversationResource;

        // Add serviceEdition to Conversation, needed by receipt polling task
        conversationService.addCustomProperty(cr, "serviceEdition", cr.getServiceEdition());

        PostVirksomhetStrategyFactory dpvFactory = PostVirksomhetStrategyFactory.newInstance(props, cr);
        CorrespondenceAgencyConfiguration config = dpvFactory.getConfig();
        InsertCorrespondenceV2 message;
        message = CorrespondenceAgencyMessageFactory.create(config, cr, messagePersister);

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
