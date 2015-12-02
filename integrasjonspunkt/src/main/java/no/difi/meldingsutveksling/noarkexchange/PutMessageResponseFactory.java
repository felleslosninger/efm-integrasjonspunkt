package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType;

/**
 * Factory class for different varieties of PutMessageResponse
 * @author Glenn Bech
 */
public class PutMessageResponseFactory {

    public static PutMessageResponseType createErrorResponse(MessageSender.MessageContextException exception) {
        PutMessageResponseType response = new PutMessageResponseType();
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType("ERROR ");
        StatusMessageType statusMessageType = new StatusMessageType();
        statusMessageType.setText(exception.getStatus().getEndUserMessage());
        statusMessageType.setCode(exception.getStatus().getId());
        receipt.getMessage().add(statusMessageType);
        response.setResult(receipt);
        return response;
    }

    public static PutMessageResponseType createErrorResponse(String errorStatus) {
        PutMessageResponseType response = new PutMessageResponseType();
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType("ERROR ");
        StatusMessageType statusMessageType = new StatusMessageType();
        statusMessageType.setText(errorStatus);
        statusMessageType.setCode(String.valueOf(1));
        receipt.getMessage().add(statusMessageType);
        response.setResult(receipt);
        return response;
    }

    public static PutMessageResponseType createOkResponse() {
        PutMessageResponseType response = new PutMessageResponseType();
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType("OK");
        response.setResult(receipt);
        return response;
    }
}
