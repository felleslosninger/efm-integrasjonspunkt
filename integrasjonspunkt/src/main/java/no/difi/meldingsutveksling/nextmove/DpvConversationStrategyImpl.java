package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.dpv.AltinnDPVService;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.DpvConversationStrategy;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static no.difi.meldingsutveksling.receipt.ReceiptStatus.*;

@Component
@Slf4j
@ConditionalOnProperty(name = "difi.move.feature.enableDPV", havingValue = "true")
@Order
@RequiredArgsConstructor
public class DpvConversationStrategyImpl implements DpvConversationStrategy {

    private final ConversationService conversationService;
    private final AltinnDPVService altinnService;

    @Override
    @Transactional
    @Timed
    public void send(@NotNull NextMoveOutMessage message) {

        if (SBDUtil.isReceipt(message.getSbd())) {
            log.info("Message [%s] is a receipt - not supported by DPV. Discarding message.".formatted(message.getMessageId()));
            conversationService.registerStatus(message.getMessageId(), SENDT, LEVERT, LEST);
            return;
        }

        UUID correspondenceid = altinnService.send(message);


//        InsertCorrespondenceV2 correspondence = correspondenceAgencyMessageFactory.create(message);
//
//        Object response = withLogstashMarker(markerFrom(message))
//                .execute(() -> client.sendCorrespondence(correspondence));

//        if (response == null) {
//            throw new NextMoveRuntimeException("Failed to create Correspondence Agency Request");
//        }

        conversationService.findConversation(message.getMessageId())
            .ifPresent(conversation -> conversationService.save(conversation
                .setExternalSystemReference(correspondenceid.toString())));
    }
}
