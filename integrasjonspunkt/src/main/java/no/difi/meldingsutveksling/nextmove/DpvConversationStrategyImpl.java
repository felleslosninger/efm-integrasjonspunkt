package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.altinnv3.DPV.AltinnUploadService;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.DpvConversationStrategy;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondencesResponseExt;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;
import static no.difi.meldingsutveksling.ptv.WithLogstashMarker.withLogstashMarker;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.*;

@Component
@Slf4j
@ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
@Order
@RequiredArgsConstructor
public class DpvConversationStrategyImpl implements DpvConversationStrategy {

    private final CorrespondenceAgencyMessageFactory correspondenceAgencyMessageFactory;
    private final CorrespondenceAgencyClient client;
    private final ConversationService conversationService;
    private final AltinnUploadService altinnUploadService;

    @Override
    @Transactional
    @Timed
    public void send(@NotNull NextMoveOutMessage message) {

        if (SBDUtil.isReceipt(message.getSbd())) {
            log.info("Message [%s] is a receipt - not supported by DPV. Discarding message.".formatted(message.getMessageId()));
            conversationService.registerStatus(message.getMessageId(), SENDT, LEVERT, LEST);
            return;
        }

        String resource =  altinnUploadService.send(message);

//        InsertCorrespondenceV2 correspondence = correspondenceAgencyMessageFactory.create(message);
//
//        Object response = withLogstashMarker(markerFrom(message))
//                .execute(() -> client.sendCorrespondence(correspondence));

//        if (response == null) {
//            throw new NextMoveRuntimeException("Failed to create Correspondence Agency Request");
//        }

        conversationService.findConversation(message.getMessageId())
            .ifPresent(conversation -> conversationService.save(conversation
                .setResource(resource)));

//        String serviceCode = correspondence.getCorrespondence().getServiceCode().getValue();
//        String serviceEditionCode = correspondence.getCorrespondence().getServiceEdition().getValue();
//        conversationService.findConversation(message.getMessageId())
//                .ifPresent(conversation -> conversationService.save(conversation
//                        .setServiceCode(serviceCode)
//                        .setServiceEditionCode(serviceEditionCode)));
    }
}
