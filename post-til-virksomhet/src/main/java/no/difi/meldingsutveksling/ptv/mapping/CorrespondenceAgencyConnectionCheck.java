package no.difi.meldingsutveksling.ptv.mapping;

import lombok.RequiredArgsConstructor;
import no.altinn.services.serviceengine.correspondence._2009._10.GetCorrespondenceStatusDetailsV2;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.receipt.Conversation;

import javax.annotation.PostConstruct;

@RequiredArgsConstructor
public class CorrespondenceAgencyConnectionCheck {

    private final UUIDGenerator uuidGenerator;
    private final CorrespondenceAgencyClient correspondenceAgencyClient;
    private final CorrespondenceAgencyMessageFactory correspondenceAgencyMessageFactory;

    @PostConstruct
    public void checkTheConnection() {
        try {
            Object response = correspondenceAgencyClient.sendStatusRequest(getConnectionCheckRequest());

            if (response == null) {
                // Error is picked up by soap fault interceptor
                throw new NextMoveRuntimeException("Null response from CorrespondenceAgency");
            }
        } catch (Exception e) {
            throw new NextMoveRuntimeException("Couldn't connect to CorrespondenceAgency", e);
        }
    }

    private GetCorrespondenceStatusDetailsV2 getConnectionCheckRequest() {
        Conversation c = new Conversation();
        c.setConversationId(uuidGenerator.generate());
        c.setServiceCode("4255");
        c.setServiceEditionCode("10");
        return correspondenceAgencyMessageFactory.createReceiptRequest(c);
    }
}
