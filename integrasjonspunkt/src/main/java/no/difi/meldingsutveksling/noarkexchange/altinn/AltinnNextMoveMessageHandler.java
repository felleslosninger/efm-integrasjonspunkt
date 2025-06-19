package no.difi.meldingsutveksling.noarkexchange.altinn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.DPO.AltinnPackage;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringMessage;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class AltinnNextMoveMessageHandler implements AltinnMessageHandler {

    private final ConversationService conversationService;
    private final NextMoveQueue nextMoveQueue;

    @Override
    public void handleAltinnPackage(AltinnPackage altinnPackage) {
        StandardBusinessDocument sbd = altinnPackage.getSbd();
        Resource asic = altinnPackage.getAsic();
        log.debug("NextMove message id=%s".formatted(sbd.getMessageId()));

        nextMoveQueue.enqueueIncomingMessage(sbd, DPO, asic);
        if (altinnPackage.getTmpFile() != null) {
            altinnPackage.getTmpFile().delete();
        }

        if (SBDUtil.isReceipt(sbd)) {
            sbd.getBusinessMessage(ArkivmeldingKvitteringMessage.class).ifPresent(receipt ->
                conversationService.registerStatus(receipt.getRelatedToMessageId(), toReceiptStatus(receipt),
                        toDescription(receipt), toRawReceipt(receipt))
            );
        }
    }

    private ReceiptStatus toReceiptStatus(ArkivmeldingKvitteringMessage arkivmeldingKvittering) {
        if ("ERROR".equals(arkivmeldingKvittering.getReceiptType())) {
            return ReceiptStatus.FEIL;
        } else if ("NOTSUPPORTED".equals(arkivmeldingKvittering.getReceiptType())) {
            return ReceiptStatus.FEIL;
        } else {
            // Tolker OK, WARNING og eventuelle ukjente verdier som LEST
            return ReceiptStatus.LEST;
        }
    }

    private String toDescription(ArkivmeldingKvitteringMessage arkivmeldingKvittering) {
        return "ArkivmeldingKvittering: " + arkivmeldingKvittering.getReceiptType();
    }

    private String toRawReceipt(ArkivmeldingKvitteringMessage arkivmeldingKvittering) {
        try {
            return new ObjectMapper().writeValueAsString(arkivmeldingKvittering);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

}
