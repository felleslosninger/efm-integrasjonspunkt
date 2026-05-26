package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.DphPolling;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dph.DphService;
import no.difi.meldingsutveksling.dph.client.DphClientService;
import no.difi.meldingsutveksling.dph.client.DphException;
import no.difi.meldingsutveksling.dph.client.domain.ApplicationReceiptResponse;
import no.difi.meldingsutveksling.dph.client.domain.BusinessDocumentResponse;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.sbd.ScopeFactory;
import no.difi.meldingsutveksling.status.Conversation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Order
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPH", havingValue = "true")
@RequiredArgsConstructor
public class DefaultDphPolling implements DphPolling {

    private final DphService dphService;
    private final SBDFactory sbdFactory;
    private final NextMoveQueue nextMoveQueue;
    private final DphClientService dphClientService;
    private final ConversationService conversationService;
    private final NextMoveMessageService nextMoveMessageService;
    private final IntegrasjonspunktProperties properties;

    @Override
    @Timed
    public void poll() {
        properties.getDph().getHerIds().forEach(this::pollForHerId);
    }

    private void pollForHerId(@NotNull Integer herId) {
        Iso6523 onBehalfOf = dphService.getOnBehalfOf(NhnIdentifier.herId(herId));
        List<IncomingMessage> messages = dphClientService.getMessages(onBehalfOf, herId);
        messages.forEach(message -> handleMessage(onBehalfOf, message));
    }

    private void handleMessage(Iso6523 onBehalfOf, IncomingMessage incomingMessage) {
        log.info("DPH message received: incomingMessage={}", incomingMessage);

        try {
            enqueueBusinessDoucment(onBehalfOf, incomingMessage);

            conversationService.findConversation(incomingMessage.getBusinessDocumentId())
                .ifPresent(conversation -> {
                    conversation.setExternalSystemReference(incomingMessage.getId());
                    conversationService.save(conversation);
                });

            if (!incomingMessage.isAppRec() && !properties.getFeature().isEnableReceipts()) {
                log.info("Sending automatic ApplicationReceipt since difi.move.feature.enable-receipts=false");

                sendMessage(sbdFactory.createNextMoveSBD(
                    NhnIdentifier.herId(incomingMessage.getReceiverHerId()),
                    NhnIdentifier.herId(incomingMessage.getSenderHerId()),
                    incomingMessage.getBusinessDocumentId(),
                    null,
                    properties.getDph().getReceiptProcess(),
                    properties.getDph().getReceiptDocumentType(),
                    new DialogmeldingKvitteringMessage()
                        .setRelatedToMessageId(incomingMessage.getId())
                        .setStatus(DialogmeldingKvitteringStatus.OK)
                ));
            }

            dphClientService.markAsRead(onBehalfOf, incomingMessage.getReceiverHerId(), incomingMessage.getId());
        } catch (DphException e) {
            DialogmeldingKvitteringMessage message = new DialogmeldingKvitteringMessage()
                .setRelatedToMessageId(incomingMessage.getId())
                .setStatus(DialogmeldingKvitteringStatus.REJECTED)
                .addMessage(new KvitteringStatusMessage()
                    .setCode(e.getErrorCode())
                    .setText(e.getMessage())
                );

            log.warn("Client error while attempting to fetch business document for incomingMessage = {} - Sending automatic ApplicationReceipt: {}", incomingMessage, message);

            StandardBusinessDocument sbd = sbdFactory.createNextMoveSBD(
                NhnIdentifier.herId(incomingMessage.getReceiverHerId()),
                NhnIdentifier.herId(incomingMessage.getSenderHerId()),
                incomingMessage.getBusinessDocumentId(),
                null,
                properties.getDph().getReceiptProcess(),
                properties.getDph().getReceiptDocumentType(),
                message
            );

            sendMessage(sbd);

            dphClientService.markAsRead(onBehalfOf, incomingMessage.getReceiverHerId(), incomingMessage.getId());
        }
    }

    private void sendMessage(StandardBusinessDocument sbd) {
        NextMoveOutMessage message = nextMoveMessageService.createMessage(sbd);
        nextMoveMessageService.sendMessage(message);
    }

    private void enqueueBusinessDoucment(Iso6523 onBehalfOf, IncomingMessage incomingMessage) {
        if (incomingMessage.isAppRec()) {
            handleApplicationReceipt(onBehalfOf, incomingMessage);
        } else {
            handleDialogmelding(onBehalfOf, incomingMessage);
        }
    }

    private void handleDialogmelding(Iso6523 onBehalfOf, IncomingMessage incomingMessage) {
        BusinessDocumentResponse response = dphClientService.receiveBusinessDocument(onBehalfOf, incomingMessage.getId());

        log.debug("DPH dialogmelding received: incomingMessage={}, response={}", incomingMessage, response);

        StandardBusinessDocument sbd = sbdFactory.createNextMoveSBD(
            NhnIdentifier.herId(incomingMessage.getSenderHerId()),
            NhnIdentifier.herId(incomingMessage.getReceiverHerId()),
            response.getConversationId(),
            incomingMessage.getBusinessDocumentId(),
            properties.getDph().getNhnProcess(),
            properties.getDph().getDialogmeldingDocumentType(),
            response.getPayload()
        );

        Optional.ofNullable(response.getParentId())
            .map(ScopeFactory::fromParentId)
            .ifPresent(sbd::addScope);

        nextMoveQueue.enqueueIncomingMessage(sbd, ServiceIdentifier.DPH, response.getEncryptedAsic());
    }

    private void handleApplicationReceipt(Iso6523 onBehalfOf, IncomingMessage incomingMessage) {
        ApplicationReceiptResponse response = dphClientService.receiveApplicationReceipt(onBehalfOf, incomingMessage.getId());
        DialogmeldingKvitteringMessage message = response.getPayload();

        String relatedToMessageId = message.getRelatedToMessageId();

        Optional<Conversation> conversation = conversationService.findConversation(relatedToMessageId);

        conversation.ifPresent(c -> {
            switch (message.getStatus()) {
                case DialogmeldingKvitteringStatus.OK ->
                    conversationService.registerStatus(relatedToMessageId, ReceiptStatus.LEVERT, "Application receipt has been recieved.", response.getRawReceipt());
                case DialogmeldingKvitteringStatus.REJECTED, DialogmeldingKvitteringStatus.OK_ERROR_IN_MESSAGE_PART ->
                    conversationService.registerStatus(relatedToMessageId, ReceiptStatus.FEIL, "Message has been rejected by the application", response.getRawReceipt());
            }
        });

        if (properties.getFeature().isEnableReceipts()) {
            StandardBusinessDocument sbd = sbdFactory.createNextMoveSBD(
                NhnIdentifier.herId(incomingMessage.getSenderHerId()),
                NhnIdentifier.herId(incomingMessage.getReceiverHerId()),
                conversation.map(Conversation::getConversationId).orElseGet(incomingMessage::getBusinessDocumentId),
                incomingMessage.getBusinessDocumentId(),
                properties.getDph().getReceiptProcess(),
                properties.getDph().getReceiptDocumentType(),
                message
            );

            nextMoveQueue.enqueueIncomingMessage(sbd, ServiceIdentifier.DPH, response.getEncryptedAsic());
        } else {
            log.info("Skipping ApplicationReceipt since difi.move.feature.enable-receipts=false");
        }
    }
}

