package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
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

    @Override
    public void send(ConversationResource conversationResource) {
        throw new UnsupportedOperationException("ConversationResource no longer in use");
    }

    @Override
    public void send(NextMoveMessage message) throws NextMoveException {

        InsertCorrespondenceV2 insertCorrespondenceV2 = correspondenceAgencyMessageFactory.create(message);

        Object response = withLogstashMarker(markerFrom(message))
                .execute(() -> client.sendCorrespondence(insertCorrespondenceV2));

        if (response == null) {
            throw new NextMoveException("Failed to create Correspondence Agency Request");
        }
    }
}
