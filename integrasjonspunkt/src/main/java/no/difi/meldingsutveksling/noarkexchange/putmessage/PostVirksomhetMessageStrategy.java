package no.difi.meldingsutveksling.noarkexchange.putmessage;

import lombok.RequiredArgsConstructor;
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
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;

import static no.difi.meldingsutveksling.core.EDUCoreMarker.markerFrom;
import static no.difi.meldingsutveksling.ptv.WithLogstashMarker.withLogstashMarker;

@RequiredArgsConstructor
public class PostVirksomhetMessageStrategy implements MessageStrategy {

    private final CorrespondenceAgencyMessageFactory correspondenceAgencyMessageFactory;
    private final CorrespondenceAgencyClient client;
    private final NoarkClient noarkClient;
    private final InternalQueue internalQueue;

    @Override
    public PutMessageResponseType send(EDUCore message) {
        final InsertCorrespondenceV2 correspondence = correspondenceAgencyMessageFactory.create(message);

        if (withLogstashMarker(markerFrom(message))
                .execute(() -> client.sendCorrespondence(correspondence)) == null) {
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
