package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType;

/**
 * Factory class for different varieties of PutMessageResponse
 * @author Glenn Bech
 */
public class PutMessageResponseFactory {

    public static PutMessageResponseType createErrorResponse(MessageException exception) {
        return createErrorResponse(exception.getStatusMessage());
    }

    public static PutMessageResponseType createErrorResponse(StatusMessage statusMessage) {
        PutMessageResponseType response = new PutMessageResponseType();
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType("ERROR");
        StatusMessageType statusMessageType = new StatusMessageType();
        statusMessageType.setText(statusMessage.getEndUserMessage());
        statusMessageType.setCode(statusMessage.getId());
        receipt.getMessage().add(statusMessageType);
        response.setResult(receipt);
        return response;
    }

    public static PutMessageResponseType createErrorResponse(String errorMsg) {
        PutMessageResponseType response = new PutMessageResponseType();
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType("ERROR");
        StatusMessageType statusMessageType = new StatusMessageType();
        statusMessageType.setText(errorMsg);
        statusMessageType.setCode("Unknown");
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
