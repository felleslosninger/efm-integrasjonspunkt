package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;
import no.difi.meldingsutveksling.ptv.mapping.CorrespondenceAgencyValues;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.noarkexchange.PutMessageMarker.markerFrom;

public class PostVirksomhetPutMessageStrategy implements PutMessageStrategy {

    private final CorrespondenceAgencyConfiguration config;
    private final ServiceRegistryLookup serviceRegistryLookup;

    public PostVirksomhetPutMessageStrategy(CorrespondenceAgencyConfiguration config, ServiceRegistryLookup serviceRegistryLookup) {
        this.config = config;
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType requestType) {
        final PutMessageRequestWrapper putMessageWrapper = new PutMessageRequestWrapper(requestType);
        InfoRecord senderInfo = serviceRegistryLookup.getInfoRecord(putMessageWrapper.getSenderPartynumber());
        InfoRecord receiverInfo = serviceRegistryLookup.getInfoRecord(putMessageWrapper.getRecieverPartyNumber());
        try {
            CorrespondenceAgencyValues values = CorrespondenceAgencyValues.from(putMessageWrapper, senderInfo, receiverInfo);
            final InsertCorrespondenceV2 message = CorrespondenceAgencyMessageFactory.create(config, values);
            CorrespondenceAgencyClient client = new CorrespondenceAgencyClient(markerFrom(putMessageWrapper));
            final CorrespondenceRequest request = new CorrespondenceRequest.Builder().withUsername(config.getSystemUserCode()).withPassword(config.getPassword()).withPayload(message).build();

            client.send(request);
            return PutMessageResponseFactory.createOkResponse();
        } catch (PayloadException e) {
            Audit.error("Unable to create message for Post til virksomhet", markerFrom(putMessageWrapper), e);
            return PutMessageResponseFactory.createErrorResponse(new MessageException(StatusMessage.POST_VIRKSOMHET_REQUEST_MISSING_VALUES));
        }
    }
}
