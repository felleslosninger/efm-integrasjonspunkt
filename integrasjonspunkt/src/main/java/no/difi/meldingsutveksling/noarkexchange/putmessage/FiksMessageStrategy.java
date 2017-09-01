package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType;
import org.assertj.core.util.Strings;

class FiksMessageStrategy implements MessageStrategy {
    private SvarUtService svarUtService;
    private NoarkClient noarkClient;

    FiksMessageStrategy(SvarUtService svarUtService, NoarkClient noarkClient) {
        this.svarUtService = svarUtService;
        this.noarkClient = noarkClient;
    }

    @Override
    public PutMessageResponseType send(EDUCore request) {

        request.setServiceIdentifier(ServiceIdentifier.DPF);
        String response = svarUtService.send(request);

        // Return AppReceipt on valid response
        if (!Strings.isNullOrEmpty(response)) {
            AppReceiptType receipt = new AppReceiptType();
            receipt.setType("OK");
            StatusMessageType statusMessageType = new StatusMessageType();
            statusMessageType.setCode("ID");
            statusMessageType.setText(response);
            receipt.getMessage().add(statusMessageType);

            // Need to marshall payload before sending to noarkClient, then set old payload back
            Object oldPayload = request.getPayload();
            request.swapSenderAndReceiver();
            request.setMessageType(EDUCore.MessageType.APPRECEIPT);
            request.setPayload(EDUCoreConverter.appReceiptAsString(receipt));
            PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(request);
            noarkClient.sendEduMelding(putMessage);
            request.setPayload(oldPayload);
        }


        return PutMessageResponseFactory.createOkResponse();
    }
}
