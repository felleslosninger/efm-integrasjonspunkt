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
import no.difi.meldingsutveksling.dph.client.domain.ApplicationReceiptResponse;
import no.difi.meldingsutveksling.dph.client.domain.BusinessDocumentResponse;
import no.difi.meldingsutveksling.dph.client.domain.SendApplicationReceiptInput;
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
        if (incomingMessage.isAppRec()) {
            handleApplicationReceipt(onBehalfOf, incomingMessage);
        } else {
            handleDialogmelding(onBehalfOf, incomingMessage);
        }

        conversationService.findConversation(incomingMessage.getBusinessDocumentId())
            .ifPresent(conversation -> {
                conversation.setExternalSystemReference(incomingMessage.getId());
                conversationService.save(conversation);
            });

        dphClientService.markAsRead(onBehalfOf, incomingMessage.getReceiverHerId(), incomingMessage.getId());

        if (!incomingMessage.isAppRec() && !properties.getFeature().isEnableReceipts()) {
            log.info("Sending automatic ApplicationReceipt since difi.move.feature.enable-receipts=false");

            dphClientService.sendApplicationReceipt(onBehalfOf, new SendApplicationReceiptInput()
                .setSenderHerId(incomingMessage.getReceiverHerId())
                .setPayload(new DialogmeldingKvitteringMessage()
                    .setRelatedToMessageId(incomingMessage.getId())
                    .setStatus(DialogmeldingKvitteringStatus.OK)
                )
            );
        }
    }

    private void handleDialogmelding(Iso6523 onBehalfOf, IncomingMessage incomingMessage) {
        BusinessDocumentResponse response = dphClientService.receiveBusinessDocument(onBehalfOf, incomingMessage.getId());

        log.debug("DPH dialogmelding received: incomingMessage={}, response={}", incomingMessage, response);

        StandardBusinessDocument sbd = sbdFactory.createNextMoveSBD(
            NhnIdentifier.herId(incomingMessage.getSenderHerId()),
            NhnIdentifier.herId(incomingMessage.getReceiverHerId()),
            Optional.ofNullable(response.getConversationId())
                .flatMap(conversationService::findConversationByExternalSystemReference)
                .map(Conversation::getConversationId)
                .orElse(response.getMessageId()),
            response.getMessageId(),
            properties.getDph().getNhnProcess(),
            properties.getDph().getDialogmeldingDocumentType(),
            response.getPayload()
        );

        Optional.ofNullable(response.getParentId())
            .flatMap(conversationService::findConversationByExternalSystemReference)
            .map(Conversation::getMessageId)
            .ifPresent(parentId -> sbd.getScopes().add(ScopeFactory.fromParentId(parentId)));

        nextMoveQueue.enqueueIncomingMessage(sbd, ServiceIdentifier.DPH, response.getEncryptedAsic());
    }

    private void handleApplicationReceipt(Iso6523 onBehalfOf, IncomingMessage incomingMessage) {
        ApplicationReceiptResponse response = dphClientService.receiveApplicationReceipt(onBehalfOf, incomingMessage.getId());
        DialogmeldingKvitteringMessage message = response.getPayload();

        conversationService.findConversation(message.getRelatedToMessageId()).ifPresentOrElse(conversation -> {
            String messageId = conversation.getMessageId();

            switch (message.getStatus()) {
                case DialogmeldingKvitteringStatus.OK ->
                    conversationService.registerStatus(messageId, ReceiptStatus.LEVERT, "Application receipt has been recieved.", response.getRawReceipt());
                case DialogmeldingKvitteringStatus.REJECTED, DialogmeldingKvitteringStatus.OK_ERROR_IN_MESSAGE_PART ->
                    conversationService.registerStatus(messageId, ReceiptStatus.FEIL, "Message has been rejected by the application", response.getRawReceipt());
            }

            if (properties.getFeature().isEnableReceipts()) {
                StandardBusinessDocument sbd = sbdFactory.createNextMoveSBD(
                    NhnIdentifier.herId(incomingMessage.getSenderHerId()),
                    NhnIdentifier.herId(incomingMessage.getReceiverHerId()),
                    conversation.getConversationId(),
                    response.getMessageId(),
                    properties.getDph().getReceiptProcess(),
                    properties.getDph().getReceiptDocumentType(),
                    message
                );

                message.setRelatedToMessageId(conversation.getMessageId());
                nextMoveQueue.enqueueIncomingMessage(sbd, ServiceIdentifier.DPH, response.getEncryptedAsic());
            } else {
                log.info("Skipping ApplicationReceipt since difi.move.feature.enable-receipts=false");
            }
        }, () -> log.warn("Conversation not found for relatedToMessageId = {}", message.getRelatedToMessageId()));
    }
}

