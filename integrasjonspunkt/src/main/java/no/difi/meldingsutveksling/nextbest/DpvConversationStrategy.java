package no.difi.meldingsutveksling.nextbest;

import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextbest.logging.ConversationResourceMarkers;
import no.difi.meldingsutveksling.noarkexchange.putmessage.PostVirksomhetStrategyFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DpvConversationStrategy implements ConversationStrategy {

    private static final Logger log = LoggerFactory.getLogger(DpvConversationStrategy.class);

    private IntegrasjonspunktProperties props;
    private ServiceRegistryLookup sr;

    @Autowired
    DpvConversationStrategy(IntegrasjonspunktProperties props,
                            ServiceRegistryLookup sr) {
        this.props = props;
        this.sr = sr;
    }

    @Override
    public ResponseEntity send(ConversationResource conversationResource) {
        DpvConversationResource cr = (DpvConversationResource) conversationResource;

        PostVirksomhetStrategyFactory dpvFactory = PostVirksomhetStrategyFactory.newInstance(props, sr);
        CorrespondenceAgencyConfiguration config = dpvFactory.getConfig();
        InsertCorrespondenceV2 message;
        try {
            message = CorrespondenceAgencyMessageFactory.create(config, cr);
        } catch (NextMoveException e) {
            log.error("Failed to create CorrespondenceAgency message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        ServiceRecord serviceRecord = sr.getServiceRecord(cr.getReceiverId());
        CorrespondenceAgencyClient client = new CorrespondenceAgencyClient(ConversationResourceMarkers.markerFrom(cr),
                config,
                serviceRecord.getEndPointURL());
        final CorrespondenceRequest request = new CorrespondenceRequest.Builder()
                .withUsername(config.getSystemUserCode())
                .withPassword(config.getPassword())
                .withPayload(message).build();

        if (client.sendCorrespondence(request) == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create Correspondence " +
                    "Agency Request");
        }

        return ResponseEntity.ok().build();
    }

}
