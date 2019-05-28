package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.noarkexchange.AppReceiptFactory;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;
import static no.difi.meldingsutveksling.ptv.WithLogstashMarker.withLogstashMarker;

@Component
@RequiredArgsConstructor
public class DpvConversationStrategy implements ConversationStrategy {

    private final CorrespondenceAgencyMessageFactory correspondenceAgencyMessageFactory;
    private final CorrespondenceAgencyClient client;
    private final IntegrasjonspunktProperties props;
    private final EDUCoreFactory eduCoreFactory;
    private final NoarkClient localNoark;

    @Override
    public void send(ConversationResource conversationResource) {
        throw new UnsupportedOperationException("ConversationResource no longer in use");
    }

    @Override
    public void send(NextMoveOutMessage message) throws NextMoveException {

        InsertCorrespondenceV2 correspondence = correspondenceAgencyMessageFactory.create(message);

        Object response = withLogstashMarker(markerFrom(message))
                .execute(() -> client.sendCorrespondence(correspondence));

        if (response == null) {
            throw new NextMoveException("Failed to create Correspondence Agency Request");
        }

        if (props.getNoarkSystem().isEnable()) {
            sendAppReceipt(message);
        }
    }

    private void sendAppReceipt(NextMoveOutMessage message) {
        AppReceiptType appReceipt = AppReceiptFactory.from("OK", "None", "OK");
        EDUCore eduCore = eduCoreFactory.create(appReceipt, message.getConversationId(),
                message.getReceiverIdentifier(), message.getSenderIdentifier());
        PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(eduCore);
        localNoark.sendEduMelding(putMessage);
    }
}
