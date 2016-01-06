package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;

/**
 * Created by steinbjarne
 */
public class PutMessageObjectMother {
    public static PutMessageRequestType createMessageRequestType(String orgNumber) {
        PutMessageRequestType request = new PutMessageRequestType();
        EnvelopeType envelope = new EnvelopeType();
        AddressType address = new AddressType();
        address.setOrgnr(orgNumber);
        envelope.setSender(address);
        request.setEnvelope(envelope);
        request.setPayload("<Melding><journpost><jpId>5</jpId></journpost></Melding>");
        return request;
    }
}
