package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.putmessage.ErrorStatus;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType;

/**
 * Factory class for different varieties of PutMessageResponse
 * @author Glenn Bech
 */
public class PutMessageResponseFactory {

    public static PutMessageResponseType createErrorResponse(ErrorStatus errorStatus) {
        PutMessageResponseType response = new PutMessageResponseType();
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType("ERROR ");
        StatusMessageType statusMessageType = new StatusMessageType();
        statusMessageType.setText(errorStatus.enduserErrorMessage());
        statusMessageType.setCode(String.valueOf(errorStatus.id));
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
