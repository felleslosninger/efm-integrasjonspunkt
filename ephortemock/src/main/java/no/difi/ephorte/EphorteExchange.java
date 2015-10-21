package no.difi.ephorte;

import no.difi.meldingsutveksling.noarkexchange.ephorte.schema.*;

import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(endpointInterface = "no.difi.meldingsutveksling.noarkexchange.ephorte.schema.NoarkExchangeBinding")
public class EphorteExchange implements NoarkExchangeBinding {

    @Override
    public GetCanReceiveMessageResponseType getCanReceiveMessage(@WebParam(name="GetCanReceiveMessageRequest", targetNamespace = "http://www.arkivverket.no/Noark/Exchange/types", partName = "getCanReceiveMessageRequest") GetCanReceiveMessageRequestType getCanReceiveMessageRequest) {
        GetCanReceiveMessageResponseType response = new GetCanReceiveMessageResponseType();
        response.setResult(true);
        return response;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType putMessageRequest) {
        PutMessageResponseType response = new PutMessageResponseType();
        AppReceiptType receiptType = new AppReceiptType();
        receiptType.setType("Hello world");
        response.setResult(receiptType);
        return response;
    }
}
