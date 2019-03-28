package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;

import java.time.Clock;

import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;

public class PostVirksomhetMessageStrategy implements MessageStrategy {

    private final CorrespondenceAgencyConfiguration config;
    private final Clock clock;
    private final NoarkClient noarkClient;
    private final InternalQueue internalQueue;

    public PostVirksomhetMessageStrategy(CorrespondenceAgencyConfiguration config,
                                         Clock clock, NoarkClient noarkClient,
                                         InternalQueue internalQueue) {
        this.config = config;
        this.clock = clock;
        this.noarkClient = noarkClient;
        this.internalQueue = internalQueue;
    }

    @Override
    public PutMessageResponseType send(EDUCore message) {
        final InsertCorrespondenceV2 correspondence = CorrespondenceAgencyMessageFactory.create(config, clock, message);
        CorrespondenceAgencyClient client = new CorrespondenceAgencyClient(markerFrom(message), config);
        final CorrespondenceRequest request = new CorrespondenceRequest.Builder()
                .withUsername(config.getSystemUserCode())
                .withPassword(config.getPassword())
                .withPayload(correspondence).build();

        if (client.sendCorrespondence(request) == null) {
            return PutMessageResponseFactory.createErrorResponse(StatusMessage.DPV_REQUEST_MISSING_VALUES);
        }

        if (noarkClient != null) {
            AppReceiptType receipt = new AppReceiptType();
            receipt.setType("OK");
            StatusMessageType statusMessageType = new StatusMessageType();
            statusMessageType.setCode("ID");
            statusMessageType.setText("OK");
            receipt.getMessage().add(statusMessageType);

            // Need to marshall payload before sending to noarkClient, then set old payload back
            Object oldPayload = message.getPayload();
            message.swapSenderAndReceiver();
            message.setMessageType(EDUCore.MessageType.APPRECEIPT);
            message.setPayload(EDUCoreConverter.appReceiptAsString(receipt));
            PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(message);
            internalQueue.enqueuePutMessage(putMessage);
            message.setPayload(oldPayload);
            message.setMessageType(EDUCore.MessageType.EDU);
            message.swapSenderAndReceiver();
        }

        return PutMessageResponseFactory.createOkResponse();
    }

    @Override
    public String serviceName() {
        return "DPV";
    }
}
