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
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Receives DPH messages by polling the NHN adapter for the unread notifications of each HER-id.
 * <p>
 * A NewMessage notification refers to a message whose business document is fetched and put on the local queue. An
 * automatic ApplicationReceipt is sent if needed, the message is marked as downloaded, which deletes the business
 * document in MSH, and finally the notification is deleted. A notification stays unread until it is deleted, so a
 * notification that could not be handled is retried on the next poll.
 * <p>
 * Since these are separate steps, a notification may come back for a message that has already been handled. Within
 * the lifetime of this instance, the steps that are done are remembered, so only the remaining steps are retried. A
 * message that has been put on the local queue is also recognized by its conversation, so it is never received or
 * rejected again.
 * <p>
 * A message that is not found, or that cannot be fetched because of a temporary error, is retried until
 * {@code difi.move.dph.notification-retry-period} has passed since MSH created the notification. After that, a message
 * that cannot be fetched is rejected, and a notification that still cannot be handled is deleted.
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

    static final String EXPIRED_TEXT = "Meldingen var utløpt da den ble mottatt, og er ikke levert til mottakers fagsystem";

    private final Clock clock;
    private final DphService dphService;
    private final SBDFactory sbdFactory;
    private final SBDService sbdService;
    private final NextMoveQueue nextMoveQueue;
    private final DphClientService dphClientService;
    private final ConversationService conversationService;
    private final NextMoveMessageService nextMoveMessageService;
    private final IntegrasjonspunktProperties properties;

    /**
     * The messages that have been received or rejected, but whose notifications have not been deleted yet, by NHN
     * message id.
     */
    private final Map<String, HandledMessage> handledMessages = new ConcurrentHashMap<>();

    @Override
    @Timed
    public void poll() {
        forgetHandledMessagesWhoseNotificationsNeverCameBack();

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

        List<Notification> handled = new ArrayList<>();
        for (Notification notification : notifications) {
            try {
                handleNotification(onBehalfOf, herId, notification);
                handled.add(notification);
            } catch (RetryLaterException e) {
                log.info("DPH - {} The notification will be retried on the next poll: notification = {}", e.getMessage(), notification);
            } catch (Exception e) {
                if (isRetryPeriodOver(notification)) {
                    log.error("DPH - Handling notification failed, and the retry period of {} is over. " +
                        "Giving up, the notification is deleted: notification = {}", getRetryPeriod(), notification, e);
                    handled.add(notification);
                } else {
                    log.error("DPH - Handling notification failed. It will be retried on the next poll: notification = {}", notification, e);
                }
            }
        }

        deleteNotifications(onBehalfOf, handled);
    }

    private void handleNotification(Iso6523 onBehalfOf, Integer herId, Notification notification) {
        switch (notification.getType()) {
            case NEW_MESSAGE -> handleNewMessage(onBehalfOf, herId, notification);
            // MSH refused the message before it reached us, so there is no business document to fetch or answer
            case REFUSED_MESSAGE ->
                log.warn("DPH - MSH refused a message to HER-id = {}: notification = {}", herId, notification);
            // The statuses of sent messages are fetched by DphStatusStrategy, so these are only acknowledged
            case MESSAGE_SENT_STATE_UPDATED, MESSAGE_APPREC_INFO_UPDATED, MESSAGE_DELIVERY_STATE_UPDATED ->
                log.debug("DPH - Status of a sent message updated: notification = {}", notification);
        }
    }

    private void handleNewMessage(Iso6523 onBehalfOf, Integer herId, Notification notification) {
        String messageId = notification.getRelatedMessageId();
        if (messageId == null) {
            log.warn("DPH - Ignoring NewMessage notification without message id: notification = {}", notification);
            return;
        }

        HandledMessage handled = handledMessages.get(messageId);
        if (handled == null) {
            Optional<HandledMessage> received = receiveOrReject(onBehalfOf, herId, notification, messageId);
            if (received.isEmpty()) {
                return;
            }
            handled = received.get();
            handledMessages.put(messageId, handled);
        } else {
            log.info("DPH message has already been handled, only the remaining steps are retried: incomingMessage = {}", handled.message);
        }

        complete(onBehalfOf, handled);
    }

    /**
     * @return the received or rejected message, or nothing if there is no message to complete
     */
    private Optional<HandledMessage> receiveOrReject(Iso6523 onBehalfOf, Integer herId, Notification notification, String messageId) {
        IncomingMessage incomingMessage;
        try {
            incomingMessage = dphClientService.getMessage(onBehalfOf, herId, messageId);
        } catch (DphException e) {
            if (!e.isNotFound()) {
                throw e;
            }
            // MSH may announce a message before it can be read, so it is not given up at once
            if (!isRetryPeriodOver(notification)) {
                throw new RetryLaterException("The message of the notification is not found in MSH.");
            }
            log.warn("DPH - The message of the notification has not been found in MSH within the retry period of {}. " +
                "It has probably been received already, so the notification is deleted: notification = {}", getRetryPeriod(), notification);
            return Optional.empty();
        }
        log.info("DPH message received: incomingMessage={}", incomingMessage);

        if (isAlreadyReceived(incomingMessage)) {
            log.info("DPH message has already been received, and is only marked as downloaded: incomingMessage = {}", incomingMessage);
            return Optional.of(new HandledMessage(incomingMessage, null, OffsetDateTime.now(clock)));
        }

        DialogmeldingKvitteringMessage applicationReceipt;
        try {
            boolean expired = receiveMessage(onBehalfOf, incomingMessage);
            applicationReceipt = getApplicationReceiptForReceived(incomingMessage, expired);
        } catch (Exception e) {
            if (e instanceof DphException de && de.isNotFound()) {
                // Rejecting the message now would answer a message we may already have received
                log.warn("DPH - The business document no longer exists in MSH. It has probably been downloaded " +
                    "already, so the message is not rejected: incomingMessage = {}", incomingMessage, e);
                return Optional.empty();
            }
            if (isTemporary(e) && !isRetryPeriodOver(notification)) {
                log.warn("DPH - Temporary error while receiving message, it is not rejected: incomingMessage = {}", incomingMessage, e);
                throw new RetryLaterException("Receiving the message failed because of a temporary error.");
            }
            applicationReceipt = getApplicationReceiptForRejected(incomingMessage, e);
        }

        return Optional.of(new HandledMessage(incomingMessage, applicationReceipt, OffsetDateTime.now(clock)));
    }

    /**
     * Sends the ApplicationReceipt, if any, and marks the message as downloaded. A step that is done is not repeated
     * if the notification comes back.
     */
    private void complete(Iso6523 onBehalfOf, HandledMessage handled) {
        if (handled.pendingApplicationReceipt != null) {
            sendApplicationReceipt(handled.message, handled.pendingApplicationReceipt);
            handled.pendingApplicationReceipt = null;
        }
        if (!handled.downloaded) {
            markAsDownloaded(onBehalfOf, handled.message);
            handled.downloaded = true;
        }
    }

    /**
     * The notification of a received message comes back if deleting it failed. The message is put on the local queue
     * with the id of its business document. Its conversation is incoming, unless the sender is on this
     * integrasjonspunkt too: then the conversation of the sent message is reused, and gets the status
     * INNKOMMENDE_MOTTATT when the message is received.
     */
    private boolean isAlreadyReceived(IncomingMessage incomingMessage) {
        return conversationService.findConversation(incomingMessage.getBusinessDocumentId())
            .filter(conversation -> conversation.getDirection() == ConversationDirection.INCOMING
                || hasStatus(conversation, ReceiptStatus.INNKOMMENDE_MOTTATT))
            .isPresent();
    }

    private static boolean hasStatus(Conversation conversation, ReceiptStatus status) {
        return conversation.getMessageStatuses() != null && conversation.findStatus(status) != null;
    }

    /**
     * @return the ApplicationReceipt to send for a received message, if any
     */
    private DialogmeldingKvitteringMessage getApplicationReceiptForReceived(IncomingMessage incomingMessage, boolean expired) {
        if (incomingMessage.isAppRec()) {
            return null;
        }
        if (expired) {
            // The message never reaches the fagsystem, so the sender would get no ApplicationReceipt from it
            log.warn("DPH message had expired when it was received, and is rejected: incomingMessage = {}", incomingMessage);
            return new DialogmeldingKvitteringMessage()
                .setRelatedToMessageId(incomingMessage.getId())
                .setStatus(DialogmeldingKvitteringStatus.REJECTED)
                .addMessage(new KvitteringStatusMessage()
                    .setCode(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL.getVerdi())
                    .setText(EXPIRED_TEXT));
        }
        if (!properties.getFeature().isEnableReceipts()) {
            log.info("Sending automatic ApplicationReceipt since difi.move.feature.enable-receipts=false");
            return new DialogmeldingKvitteringMessage()
                .setRelatedToMessageId(incomingMessage.getId())
                .setStatus(DialogmeldingKvitteringStatus.OK);
        }
        return null;
    }

    /**
     * @return the ApplicationReceipt that rejects the message, or nothing if the message is an ApplicationReceipt
     */
    private DialogmeldingKvitteringMessage getApplicationReceiptForRejected(IncomingMessage incomingMessage, Exception e) {
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

        if (incomingMessage.isAppRec()) {
            // An ApplicationReceipt is never answered with another ApplicationReceipt, to avoid loops between the parties
            log.warn("Not sending ApplicationReceipt in response to rejected ApplicationReceipt: incomingMessage = {}, error = {}", incomingMessage, error);
            return null;
        }

        DialogmeldingKvitteringMessage message = new DialogmeldingKvitteringMessage()
            .setRelatedToMessageId(incomingMessage.getId())
            .setStatus(DialogmeldingKvitteringStatus.REJECTED)
            .addMessage(error);
        log.warn("Sending automatic ApplicationReceipt for rejected incomingMessage = {}: {}", incomingMessage, message);
        return message;
    }

    /**
     * Whether the error may go away by itself: network errors, server errors from the adapter, and database
     * conflicts, e.g. when another instance received the same message at the same time.
     */
    static boolean isTemporary(Throwable e) {
        Set<Throwable> seen = new HashSet<>();
        for (Throwable t = e; t != null && seen.add(t); t = t.getCause()) {
            if (t instanceof DphException de) {
                Integer statusCode = de.getStatusCode();
                return statusCode != null && (statusCode >= 500 || statusCode == 408 || statusCode == 429);
            }
            if (t instanceof WebClientRequestException
                || t instanceof IOException
                || t instanceof TimeoutException
                || t instanceof TransientDataAccessException
                || t instanceof DataIntegrityViolationException) {
                return true;
            }
        }
        return false;
    }

    private boolean isRetryPeriodOver(Notification notification) {
        OffsetDateTime createdAt = notification.getCreatedAt();
        // Without a creation time, there is no retry period to wait for
        return createdAt == null || createdAt.plus(getRetryPeriod()).isBefore(OffsetDateTime.now(clock));
    }

    private Duration getRetryPeriod() {
        return properties.getDph().getNotificationRetryPeriod();
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

    private void deleteNotifications(Iso6523 onBehalfOf, List<Notification> notifications) {
        for (int from = 0; from < notifications.size(); from += MAX_NOTIFICATIONS_TO_DELETE) {
            List<Notification> chunk = notifications.subList(from, Math.min(from + MAX_NOTIFICATIONS_TO_DELETE, notifications.size()));
            List<String> notificationIds = chunk.stream().map(Notification::getNotificationId).toList();
            try {
                dphClientService.deleteNotifications(onBehalfOf, notificationIds);
                chunk.stream()
                    .map(Notification::getRelatedMessageId)
                    .filter(Objects::nonNull)
                    .forEach(handledMessages::remove);
            } catch (Exception e) {
                log.error("DPH - Deleting handled notifications failed. They will come back on the next poll, " +
                    "but messages already handled are not handled again: notificationIds = {}", notificationIds, e);
            }
        }
    }

    /**
     * A handled message is forgotten when its notification is deleted. If the notification was deleted some other
     * way, it is forgotten when it is so old that its notification would have been given up anyway.
     */
    private void forgetHandledMessagesWhoseNotificationsNeverCameBack() {
        OffsetDateTime limit = OffsetDateTime.now(clock).minus(getRetryPeriod().multipliedBy(2));
        handledMessages.values().removeIf(handled -> handled.handledAt.isBefore(limit));
    }

    /**
     * @return whether the message had expired, so that it is not delivered to the fagsystem
     */
    private boolean receiveMessage(Iso6523 onBehalfOf, IncomingMessage incomingMessage) {
        boolean expired = enqueueBusinessDocument(onBehalfOf, incomingMessage);

        // The conversation of a sent message is left alone, since its reference is used to poll its statuses
        conversationService.findConversation(incomingMessage.getBusinessDocumentId())
            .filter(conversation -> conversation.getDirection() == ConversationDirection.INCOMING)
            .ifPresent(conversation -> {
                conversation.setExternalSystemReference(incomingMessage.getId());
                conversationService.save(conversation);
            });

        return expired;
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

    /**
     * @return whether the message had expired, so that it is not delivered to the fagsystem
     */
    private boolean enqueueBusinessDocument(Iso6523 onBehalfOf, IncomingMessage incomingMessage) {
        BusinessDocumentResponse response = dphClientService.receiveBusinessDocument(onBehalfOf, incomingMessage.getId());
        StandardBusinessDocument sbd = response.getSbd();

        log.debug("DPH {} received: incomingMessage={}, response={}", sbd.getType(), incomingMessage, response);

        return switch (BusinessMessageType.fromType(sbd.getType())) {
            case DIALOGMELDING -> handleDialogmelding(sbd, response.getEncryptedAsic());
            case DIALOGMELDING_KVITTERING -> {
                handleApplicationReceipt(sbd);
                yield false;
            }
            default -> throw new DphException(FeilmeldingForApplikasjonskvittering.IKKE_STOTTET_FORMAT);
        };
    }

    /**
     * @return whether the message had expired. The queue then only registers that it has expired.
     */
    private boolean handleDialogmelding(StandardBusinessDocument sbd, Resource encryptedAsic) {
        boolean expired = sbdService.isExpired(sbd);
        nextMoveQueue.enqueueIncomingMessage(sbd, ServiceIdentifier.DPH, encryptedAsic);
        return expired;
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

    /**
     * A message that has been received or rejected, with the steps that remain before its notification can be deleted.
     */
    private static final class HandledMessage {
        private final IncomingMessage message;
        private final OffsetDateTime handledAt;
        private DialogmeldingKvitteringMessage pendingApplicationReceipt;
        private boolean downloaded;

        private HandledMessage(IncomingMessage message, DialogmeldingKvitteringMessage pendingApplicationReceipt, OffsetDateTime handledAt) {
            this.message = message;
            this.pendingApplicationReceipt = pendingApplicationReceipt;
            this.handledAt = handledAt;
        }
    }

    /**
     * The notification cannot be handled yet, and is retried on the next poll. Not an error by itself.
     */
    private static final class RetryLaterException extends RuntimeException {
        private RetryLaterException(String message) {
            super(message);
        }
    }
}
