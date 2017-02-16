package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageRequestType;

public class GetCanReceiveObjectMother {
    public static GetCanReceiveMessageRequestType createRequest(String receiverIdentifier) {
        GetCanReceiveMessageRequestType request = new GetCanReceiveMessageRequestType();
        final AddressType receiver = new AddressType();
        receiver.setOrgnr(receiverIdentifier);
        request.setReceiver(receiver);
        return request;
    }
}
