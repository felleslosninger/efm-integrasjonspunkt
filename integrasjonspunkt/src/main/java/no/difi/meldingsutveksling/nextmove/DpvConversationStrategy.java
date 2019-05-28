package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.noarkexchange.AppReceiptFactory;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.nextmove.NextMoveMessageMarkers.markerFrom;
import static no.difi.meldingsutveksling.ptv.WithLogstashMarker.withLogstashMarker;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
@RequiredArgsConstructor
public class DpvConversationStrategy implements ConversationStrategy {

    private final CorrespondenceAgencyMessageFactory correspondenceAgencyMessageFactory;
    private final CorrespondenceAgencyClient client;
    private final IntegrasjonspunktProperties props;
    private final NoarkClient localNoark;
    private final PutMessageRequestFactory putMessageRequestFactory;

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
        PutMessageRequestType putMessage = putMessageRequestFactory.create(message.getSbd(),
                BestEduConverter.appReceiptAsString(appReceipt));
        localNoark.sendEduMelding(putMessage);
    }
}
