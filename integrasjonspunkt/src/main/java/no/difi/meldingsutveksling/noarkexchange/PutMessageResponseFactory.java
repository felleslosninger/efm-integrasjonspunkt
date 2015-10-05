package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

/**
 * Factory class for different varieties of PutMessageResponse
 * @author Glenn Bech
 */
public class PutMessageResponseFactory {

    public static PutMessageResponseType createErrorResponse(String message) {
        PutMessageResponseType response = new PutMessageResponseType();
        AppReceiptType receipt = new AppReceiptType();
        receipt.setType("ERROR ");
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
