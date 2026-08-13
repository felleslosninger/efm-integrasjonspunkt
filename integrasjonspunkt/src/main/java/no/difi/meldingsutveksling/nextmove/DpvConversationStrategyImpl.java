package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.QueueInterruptException;
import no.difi.meldingsutveksling.altinnv3.dpv.AltinnDPVService;
import no.difi.meldingsutveksling.altinnv3.dpv.CorrespondenceApiException;
import no.difi.meldingsutveksling.altinnv3.dpv.WithLogstashMarker;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.DpvConversationStrategy;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;
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

        UUID correspondenceid;
        try {
            correspondenceid = WithLogstashMarker.withLogstashMarker(markerFrom(message))
                    .execute(() -> altinnService.send(message));
        } catch (CorrespondenceApiException e) {
            if (HttpStatus.CONFLICT.equals(e.getStatusCode())) {
                // Idempotent key allerede kjent hos Altinn - correspondence ble opprettet i et tidligere forsøk
                // (f.eks. svaret gikk tapt). Behandles som en normal, vellykket sending.
                handleIdempotentKeyConflict(message, e);
                return;
            }
            if (e.getStatusCode() != null && e.getStatusCode().is4xxClientError()) {
                throw new QueueInterruptException(e.getMessage());
            }
            throw e;
        }

        conversationService.findConversation(message.getMessageId())
            .ifPresent(conversation -> conversationService.save(conversation
                .setExternalSystemReference(correspondenceid.toString())));

    }

    private void handleIdempotentKeyConflict(NextMoveOutMessage message, CorrespondenceApiException e) {
        log.warn(markerFrom(message),
                "Correspondence for message [{}] in conversation [{}] finnes allerede hos Altinn " +
                        "(idempotent key konflikt) - antar meldingen ble levert i et tidligere forsøk. " +
                        "Slår opp correspondenceId via sendersReference. Detaljer: {}",
                message.getMessageId(), message.getConversationId(), e.getMessage());

        Optional<UUID> correspondenceId;
        try {
            correspondenceId = altinnService.findExistingCorrespondenceId(message.getMessageId());
        } catch (Exception lookupException) {
            disablePolling(message, "Oppslag på sendersReference feilet: " + lookupException.getMessage());
            return;
        }

        if (correspondenceId.isEmpty()) {
            disablePolling(message, "Fant ingen correspondence hos Altinn ved oppslag på sendersReference.");
            return;
        }

        conversationService.findConversation(message.getMessageId())
                .ifPresent(conversation -> conversationService.save(conversation
                        .setExternalSystemReference(correspondenceId.get().toString())));
    }

    private void disablePolling(NextMoveOutMessage message, String reason) {
        log.error(markerFrom(message),
                "Klarte ikke å fastslå correspondenceId for message [{}] in conversation [{}] etter idempotent " +
                        "key konflikt. {} Stopper polling for denne samtalen.",
                message.getMessageId(), message.getConversationId(), reason);

        conversationService.findConversation(message.getMessageId())
                .ifPresent(conversation -> conversationService.save(conversation
                        .setPollable(false)
                        .setFinished(true)));
    }

}
