package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.DpvConversationStrategy;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers;
import no.difi.meldingsutveksling.noarkexchange.BestEduAppReceiptService;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.ptv.WithLogstashMarker.withLogstashMarker;

@Component
@Slf4j
@ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
@Order
@RequiredArgsConstructor
public class DpvConversationStrategyImpl implements DpvConversationStrategy {

    private final CorrespondenceAgencyMessageFactory correspondenceAgencyMessageFactory;
    private final CorrespondenceAgencyClient client;
    private final ConversationService conversationService;
    private final IntegrasjonspunktProperties props;
    private final PromiseMaker promiseMaker;
    private final BestEduAppReceiptService bestEduAppReceiptService;
    private final NextMoveMessageMarkers nextMoveMessageMarkers;

    @Override
    @Transactional
    @Timed
    public void send(@NotNull NextMoveOutMessage message) {

        promiseMaker.promise(reject -> {
            InsertCorrespondenceV2 correspondence = correspondenceAgencyMessageFactory.create(message, reject);

            Object response = withLogstashMarker(nextMoveMessageMarkers.markerFrom(message))
                    .execute(() -> client.sendCorrespondence(correspondence));

            if (response == null) {
                throw new NextMoveRuntimeException("Failed to create Correspondence Agency Request");
            }

            String serviceCode = correspondence.getCorrespondence().getServiceCode().getValue();
            String serviceEditionCode = correspondence.getCorrespondence().getServiceEdition().getValue();
            conversationService.findConversation(message.getMessageId())
                    .ifPresent(conversation -> conversationService.save(conversation
                            .setServiceCode(serviceCode)
                            .setServiceEditionCode(serviceEditionCode)));
            return null;
        }).await();

        if (!isNullOrEmpty(props.getNoarkSystem().getType())) {
            // Only log exceptions here to avoid sending message multiple times due to retry
            try {
                bestEduAppReceiptService.sendAppReceiptToLocalNoark(message);
            } catch (Exception e) {
                log.error(nextMoveMessageMarkers.markerFrom(message), "Error sending AppReceipt for DPV message", e);
            }
        }
    }

}
