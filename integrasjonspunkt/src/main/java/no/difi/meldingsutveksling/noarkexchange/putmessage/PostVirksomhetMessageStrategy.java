package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;

public class PostVirksomhetMessageStrategy implements MessageStrategy {

    private final CorrespondenceAgencyConfiguration config;
    private final ServiceRegistryLookup serviceRegistryLookup;

    public PostVirksomhetMessageStrategy(CorrespondenceAgencyConfiguration config, ServiceRegistryLookup serviceRegistryLookup) {
        this.config = config;
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    @Override
    public PutMessageResponseType send(EDUCore message) {
        ServiceRecord serviceRecord = this.serviceRegistryLookup.getServiceRecord(message.getReceiver().getIdentifier());
        final InsertCorrespondenceV2 correspondence = CorrespondenceAgencyMessageFactory.create(config, message);
        CorrespondenceAgencyClient client = new CorrespondenceAgencyClient(markerFrom(message), config,
                serviceRecord.getEndPointURL());
        final CorrespondenceRequest request = new CorrespondenceRequest.Builder()
                .withUsername(config.getSystemUserCode())
                .withPassword(config.getPassword())
                .withPayload(correspondence).build();

        if (client.sendCorrespondence(request) == null) {
            return PutMessageResponseFactory.createErrorResponse(StatusMessage.DPV_REQUEST_MISSING_VALUES);
        }
        return PutMessageResponseFactory.createOkResponse();
    }

    @Override
    public String serviceName() {
        return "DPV";
    }
}
