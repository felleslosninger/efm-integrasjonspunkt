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
import no.difi.meldingsutveksling.nhn.adapter.model.Notification;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.status.Conversation;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Receives DPH messages by polling the NHN adapter for the unread notifications of each HER-id.
 * <p>
 * A NewMessage notification refers to a message whose business document is fetched and put on the local queue. The
 * message is then marked as downloaded, which deletes the business document in MSH, and finally the notification is
 * deleted. The notification stays unread until it is deleted, so a notification that could not be handled is retried
 * on the next poll. Since marking the message as downloaded and deleting the notification are separate calls, the
 * notification of a message that has already been received may come back: such a message is neither received nor
 * rejected again.
 */
@Slf4j
@Order
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPH", havingValue = "true")
@RequiredArgsConstructor
public class DefaultDphPolling implements DphPolling {

    /**
     * The most notifications the adapter deletes in one request.
     */
    static final int MAX_NOTIFICATIONS_TO_DELETE = 1000;

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
                log.error("DPH - Polling for notifications failed for HER-id = {}", herId, e);
            }
        }
    }

    private void pollForHerId(@NotNull Integer herId) {
        Iso6523 onBehalfOf = dphService.getOnBehalfOf(NhnIdentifier.herId(herId));
        List<Notification> notifications = dphClientService.getUnreadNotifications(onBehalfOf, herId);

        List<String> handled = new ArrayList<>();
        for (Notification notification : notifications) {
            try {
                if (handleNotification(onBehalfOf, herId, notification)) {
                    handled.add(notification.getNotificationId());
                }
            } catch (Exception e) {
                log.error("DPH - Handling notification failed. It will be retried on the next poll: notification = {}", notification, e);
            }
        }

        deleteNotifications(onBehalfOf, handled);
    }

    /**
     * @return whether the notification has been handled, and can be deleted
     */
    private boolean handleNotification(Iso6523 onBehalfOf, Integer herId, Notification notification) {
        return switch (notification.getType()) {
            case NEW_MESSAGE -> handleNewMessage(onBehalfOf, herId, notification);
            case REFUSED_MESSAGE -> {
                // MSH refused the message before it reached us, so there is no business document to fetch or answer
                log.warn("DPH - MSH refused a message to HER-id = {}: notification = {}", herId, notification);
                yield true;
            }
            // The statuses of sent messages are fetched by DphStatusStrategy, so these are only acknowledged
            case MESSAGE_SENT_STATE_UPDATED, MESSAGE_APPREC_INFO_UPDATED, MESSAGE_DELIVERY_STATE_UPDATED -> {
                log.debug("DPH - Status of a sent message updated: notification = {}", notification);
                yield true;
            }
        };
    }

    private boolean handleNewMessage(Iso6523 onBehalfOf, Integer herId, Notification notification) {
        String messageId = notification.getRelatedMessageId();
        if (messageId == null) {
            log.warn("DPH - Ignoring NewMessage notification without message id: notification = {}", notification);
            return true;
        }

        Optional<IncomingMessage> found = getMessage(onBehalfOf, herId, messageId);
        if (found.isEmpty()) {
            log.warn("DPH - The message of the notification no longer exists in MSH. It has probably been received " +
                "already, so the notification is deleted: notification = {}", notification);
            return true;
        }
        IncomingMessage incomingMessage = found.get();
        log.info("DPH message received: incomingMessage={}", incomingMessage);

        if (isAlreadyReceived(incomingMessage)) {
            log.info("DPH message has already been received, and is only marked as downloaded: incomingMessage = {}", incomingMessage);
            markAsDownloaded(onBehalfOf, incomingMessage);
            return true;
        }

        try {
            receiveMessage(onBehalfOf, incomingMessage);
        } catch (DphException e) {
            if (e.isNotFound()) {
                // Rejecting the message now would answer a message we may already have received
                log.warn("DPH - The business document no longer exists in MSH. It has probably been downloaded " +
                    "already, so the message is not rejected: incomingMessage = {}", incomingMessage, e);
                return true;
            }
            return rejectMessage(onBehalfOf, incomingMessage, e);
        } catch (Exception e) {
            return rejectMessage(onBehalfOf, incomingMessage, e);
        }

        if (!incomingMessage.isAppRec() && !properties.getFeature().isEnableReceipts()) {
            sendAutomaticApplicationReceipt(incomingMessage);
        }

        markAsDownloaded(onBehalfOf, incomingMessage);
        return true;
    }

    private Optional<IncomingMessage> getMessage(Iso6523 onBehalfOf, Integer herId, String messageId) {
        try {
            return Optional.of(dphClientService.getMessage(onBehalfOf, herId, messageId));
        } catch (DphException e) {
            if (e.isNotFound()) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * The notification of a received message comes back if deleting it failed. The message is put on the local queue
     * with the id of its business document, so a conversation for it means that it has been received.
     */
    private boolean isAlreadyReceived(IncomingMessage incomingMessage) {
        return conversationService.findConversation(incomingMessage.getBusinessDocumentId())
            .filter(conversation -> conversation.getDirection() == ConversationDirection.INCOMING)
            .isPresent();
    }

    private void sendAutomaticApplicationReceipt(IncomingMessage incomingMessage) {
        log.info("Sending automatic ApplicationReceipt since difi.move.feature.enable-receipts=false");

        // The message has been received, so a failing ApplicationReceipt does not stop it from being marked as
        // downloaded: when the notification comes back, the message is already received and is not answered again
        try {
            sendApplicationReceipt(incomingMessage, new DialogmeldingKvitteringMessage()
                .setRelatedToMessageId(incomingMessage.getId())
                .setStatus(DialogmeldingKvitteringStatus.OK));
        } catch (Exception e) {
            log.error("DPH message was received, but sending the automatic ApplicationReceipt failed: incomingMessage = {}", incomingMessage, e);
        }
    }

    /**
     * Marks the message as downloaded, which deletes its business document in MSH. A message that is not found has
     * already been marked as downloaded.
     */
    private void markAsDownloaded(Iso6523 onBehalfOf, IncomingMessage incomingMessage) {
        try {
            dphClientService.markAsDownloaded(onBehalfOf, incomingMessage.getReceiverHerId(), incomingMessage.getId());
        } catch (DphException e) {
            if (!e.isNotFound()) {
                throw e;
            }
            log.info("DPH message has already been marked as downloaded: incomingMessage = {}", incomingMessage);
        }
    }

    private void deleteNotifications(Iso6523 onBehalfOf, List<String> notificationIds) {
        for (int from = 0; from < notificationIds.size(); from += MAX_NOTIFICATIONS_TO_DELETE) {
            List<String> chunk = notificationIds.subList(from, Math.min(from + MAX_NOTIFICATIONS_TO_DELETE, notificationIds.size()));
            try {
                dphClientService.deleteNotifications(onBehalfOf, chunk);
            } catch (Exception e) {
                log.error("DPH - Deleting handled notifications failed. They will come back on the next poll, " +
                    "but messages already received are not received again: notificationIds = {}", chunk, e);
            }
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

    /**
     * @return whether the message has been rejected, and its notification can be deleted
     */
    private boolean rejectMessage(Iso6523 onBehalfOf, IncomingMessage incomingMessage, Exception e) {
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

            markAsDownloaded(onBehalfOf, incomingMessage);
            return true;
        } catch (Exception ex) {
            log.error("Failed to reject DPH message. The message will be retried on the next poll: incomingMessage = {}", incomingMessage, ex);
            return false;
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
