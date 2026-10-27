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
import no.difi.meldingsutveksling.dph.client.domain.BusinessDocumentResponse;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.status.Conversation;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
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
        for (Integer herId : properties.getDph().getHerIds()) {
            try {
                pollForHerId(herId);
            } catch (Exception e) {
                log.error("DPH - Polling for messages failed for HER-id = {}", herId, e);
            }
        }
    }

    private void pollForHerId(@NotNull Integer herId) {
        Iso6523 onBehalfOf = dphService.getOnBehalfOf(NhnIdentifier.herId(herId));
        List<IncomingMessage> messages = dphClientService.getMessages(onBehalfOf, herId);
        messages.forEach(message -> handleMessage(onBehalfOf, message));
    }

    private void handleMessage(Iso6523 onBehalfOf, IncomingMessage incomingMessage) {
        log.info("DPH message received: incomingMessage={}", incomingMessage);

        try {
            receiveMessage(onBehalfOf, incomingMessage);
        } catch (Exception e) {
            rejectMessage(onBehalfOf, incomingMessage, e);
            return;
        }

        try {
            if (!incomingMessage.isAppRec() && !properties.getFeature().isEnableReceipts()) {
                log.info("Sending automatic ApplicationReceipt since difi.move.feature.enable-receipts=false");

                sendApplicationReceipt(incomingMessage, new DialogmeldingKvitteringMessage()
                    .setRelatedToMessageId(incomingMessage.getId())
                    .setStatus(DialogmeldingKvitteringStatus.OK));
            }

            dphClientService.markAsRead(onBehalfOf, incomingMessage.getReceiverHerId(), incomingMessage.getId());
        } catch (Exception e) {
            log.error("DPH message was received, but sending ApplicationReceipt or marking it as read failed. " +
                "The message will be retried on the next poll: incomingMessage = {}", incomingMessage, e);
        }
    }

    private void receiveMessage(Iso6523 onBehalfOf, IncomingMessage incomingMessage) {
        enqueueBusinessDoucment(onBehalfOf, incomingMessage);

        conversationService.findConversation(incomingMessage.getBusinessDocumentId())
            .ifPresent(conversation -> {
                conversation.setExternalSystemReference(incomingMessage.getId());
                conversationService.save(conversation);
            });
    }

    private void rejectMessage(Iso6523 onBehalfOf, IncomingMessage incomingMessage, Exception e) {
        KvitteringStatusMessage error;
        if (e instanceof DphException de) {
            log.warn("Client error while attempting to fetch business document for incomingMessage = {}", incomingMessage, e);
            error = new KvitteringStatusMessage()
                .setCode(de.getErrorCode())
                .setText(de.getMessage());
        } else {
            // Details of unexpected errors are only logged, since they may reveal internal information to the sender
            log.error("Unexpected error while receiving DPH message: incomingMessage = {}", incomingMessage, e);
            error = new KvitteringStatusMessage()
                .setCode(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL.getVerdi())
                .setText(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL.getNavn());
        }

        try {
            if (incomingMessage.isAppRec()) {
                // An ApplicationReceipt is never answered with another ApplicationReceipt, to avoid loops between the parties
                log.warn("Not sending ApplicationReceipt in response to rejected ApplicationReceipt: incomingMessage = {}, error = {}", incomingMessage, error);
            } else {
                DialogmeldingKvitteringMessage message = new DialogmeldingKvitteringMessage()
                    .setRelatedToMessageId(incomingMessage.getId())
                    .setStatus(DialogmeldingKvitteringStatus.REJECTED)
                    .addMessage(error);

                log.warn("Sending automatic ApplicationReceipt for rejected incomingMessage = {}: {}", incomingMessage, message);

                sendApplicationReceipt(incomingMessage, message);
            }

            dphClientService.markAsRead(onBehalfOf, incomingMessage.getReceiverHerId(), incomingMessage.getId());
        } catch (Exception ex) {
            log.error("Failed to reject DPH message. The message will be retried on the next poll: incomingMessage = {}", incomingMessage, ex);
        }
    }

    private void sendApplicationReceipt(IncomingMessage incomingMessage, DialogmeldingKvitteringMessage message) {
        sendMessage(sbdFactory.createNextMoveSBD(
            NhnIdentifier.herId(incomingMessage.getReceiverHerId()),
            NhnIdentifier.herId(incomingMessage.getSenderHerId()),
            incomingMessage.getBusinessDocumentId(),
            null,
            properties.getDph().getReceiptProcess(),
            properties.getDph().getReceiptDocumentType(),
            message
        ));
    }

    private void sendMessage(StandardBusinessDocument sbd) {
        NextMoveOutMessage message = nextMoveMessageService.createMessage(sbd);
        nextMoveMessageService.sendMessage(message);
    }

    private void enqueueBusinessDoucment(Iso6523 onBehalfOf, IncomingMessage incomingMessage) {
        BusinessDocumentResponse response = dphClientService.receiveBusinessDocument(onBehalfOf, incomingMessage.getId());
        StandardBusinessDocument sbd = response.getSbd();

        log.debug("DPH {} received: incomingMessage={}, response={}", sbd.getType(), incomingMessage, response);

        switch (BusinessMessageType.fromType(sbd.getType())) {
            case DIALOGMELDING -> handleDialogmelding(sbd, response.getEncryptedAsic());
            case DIALOGMELDING_KVITTERING -> handleApplicationReceipt(sbd);
            default -> throw new DphException(FeilmeldingForApplikasjonskvittering.IKKE_STOTTET_FORMAT);
        }
    }

    private void handleDialogmelding(StandardBusinessDocument sbd, Resource encryptedAsic) {
        nextMoveQueue.enqueueIncomingMessage(sbd, ServiceIdentifier.DPH, encryptedAsic);
    }

    private void handleApplicationReceipt(StandardBusinessDocument sbd) {
        DialogmeldingKvitteringMessage message = sbd.getBusinessMessage(DialogmeldingKvitteringMessage.class)
            .orElseThrow(() -> new DphException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL));

        String relatedToMessageId = message.getRelatedToMessageId();
        Optional<Conversation> conversation = conversationService.findConversation(relatedToMessageId);

        conversation.ifPresent(c -> {
            switch (message.getStatus()) {
                case DialogmeldingKvitteringStatus.OK ->
                    conversationService.registerStatus(relatedToMessageId, ReceiptStatus.LEVERT, "Application receipt has been recieved.", message.getRawReceipt());
                case DialogmeldingKvitteringStatus.REJECTED, DialogmeldingKvitteringStatus.OK_ERROR_IN_MESSAGE_PART ->
                    conversationService.registerStatus(relatedToMessageId, ReceiptStatus.FEIL, "Message has been rejected by the application", message.getRawReceipt());
            }
        });

        if (properties.getFeature().isEnableReceipts()) {
            nextMoveQueue.enqueueIncomingMessage(sbd, ServiceIdentifier.DPH);
        } else {
            log.info("Skipping ApplicationReceipt since difi.move.feature.enable-receipts=false");
        }
    }
}

