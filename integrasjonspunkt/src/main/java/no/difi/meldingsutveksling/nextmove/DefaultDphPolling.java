package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.DphPolling;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.api.NextMoveQueue.EnqueueResult;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dph.DphService;
import no.difi.meldingsutveksling.dph.client.DphClientService;
import no.difi.meldingsutveksling.dph.client.DphException;
import no.difi.meldingsutveksling.dph.client.InvalidBusinessDocumentException;
import no.difi.meldingsutveksling.dph.client.domain.BusinessDocumentResponse;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.Notification;
import no.difi.meldingsutveksling.nhn.adapter.model.NotificationType;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.status.Conversation;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Receives DPH messages by polling the NHN adapter for the unread notifications of each HER-id.
 * <p>
 * A NewMessage notification refers to a message whose business document is fetched and put on the local queue. An
 * automatic ApplicationReceipt is sent if needed, the message is marked as downloaded, which deletes the business
 * document in MSH, and finally the notification is deleted. A notification stays unread until it is deleted, so a
 * notification that could not be handled is retried on the next poll.
 * <p>
 * Since these are separate steps, a notification may come back for a message that has already been handled. Within
 * the lifetime of this instance, the steps that are done are remembered, so only the remaining steps are retried, and
 * a notification that has been handled or given up is only deleted again. A message that has been put on the local
 * queue is also recognized by its conversation, or by the local queue when another instance received it at the same
 * time, so it is never received or answered again. A rejected message gets an incoming conversation with the status
 * FEIL before it is answered, so it is recognized the same way.
 * <p>
 * Some handled messages leave no conversation of their own, so they are not recognized after a restart or on another
 * instance: a message rejected by two instances at the same time, a rejected message sent from this
 * integrasjonspunkt, whose conversation is the one of the sent message, and an ApplicationReceipt received while
 * {@code difi.move.feature.enable-receipts=false}. If the notification of such a message comes back, the message may
 * be rejected again, or its business document is not found and the notification is given up.
 * <p>
 * A message is rejected only for a fault in the message itself, at once: its business document cannot be
 * deserialized, has an unknown type, or lacks its content. Any other error says nothing about the message, so it is
 * retried, like a message that is not found, until {@code difi.move.dph.notification-retry-period} has passed since
 * this instance first failed to handle the notification. After that, the notification is deleted and the error is
 * logged, but the message is neither rejected nor marked as downloaded.
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

    /**
     * The shortest time handled messages and failing notifications are remembered, even if the retry period is
     * shorter, so that a message is not rejected again just because its notification comes back on the next poll.
     */
    static final Duration MIN_MEMORY_PERIOD = Duration.ofHours(1);

    static final String EXPIRED_TEXT = "Meldingen var utløpt da den ble mottatt, og er ikke levert til mottakers fagsystem";

    /**
     * Client errors that may go away by themselves: authorization that is not in place yet, timeouts and throttling.
     */
    private static final Set<Integer> TEMPORARY_CLIENT_ERRORS = Set.of(401, 403, 408, 429);

    private final Clock clock;
    private final DphService dphService;
    private final SBDFactory sbdFactory;
    private final NextMoveQueue nextMoveQueue;
    private final DphClientService dphClientService;
    private final ConversationService conversationService;
    private final NextMoveMessageService nextMoveMessageService;
    private final IntegrasjonspunktProperties properties;

    /**
     * The messages that have been received or rejected, but whose notifications have not been deleted yet, by HER-id
     * and NHN message id.
     */
    private final Map<String, HandledMessage> handledMessages = new ConcurrentHashMap<>();

    /**
     * When this instance first failed to handle a notification that is not handled yet, by notification id.
     */
    private final Map<String, OffsetDateTime> failingSince = new ConcurrentHashMap<>();

    /**
     * The HER-ids whose notifications could not be deleted on the last poll, so that the failure is logged in full only
     * once.
     */
    private final Set<Integer> herIdsFailingToDeleteNotifications = ConcurrentHashMap.newKeySet();

    /**
     * The HER-ids whose notifications could not be fetched on the last poll, so that the failure is logged in full only
     * once.
     */
    private final Set<Integer> herIdsFailingToPoll = ConcurrentHashMap.newKeySet();

    /**
     * The notifications that have been handled or given up, but not deleted yet, with when they were last seen, by
     * notification id. A notification that comes back is only deleted again, so that it is neither handled nor logged
     * again on every poll.
     */
    private final Map<String, OffsetDateTime> finishedNotifications = new ConcurrentHashMap<>();

    @Override
    @Timed
    public void poll() {
        forgetWhatIsTooOldToMatter();

        for (Integer herId : properties.getDph().getHerIds()) {
            try {
                pollForHerId(herId);
                if (herIdsFailingToPoll.remove(herId)) {
                    log.info("DPH - Polling for notifications works again for HER-id = {}", herId);
                }
            } catch (Exception e) {
                if (herIdsFailingToPoll.add(herId)) {
                    log.error("DPH - Polling for notifications failed for HER-id = {}", herId, e);
                } else {
                    log.debug("DPH - Polling for notifications failed again for HER-id = {}: {}", herId, e.getMessage());
                }
            }
        }
    }

    private void pollForHerId(@NotNull Integer herId) {
        Iso6523 onBehalfOf = dphService.getOnBehalfOf(NhnIdentifier.herId(herId));
        List<Notification> notifications = dphClientService.getUnreadNotifications(onBehalfOf, herId);

        List<Notification> handled = new ArrayList<>();
        for (Notification notification : notifications) {
            if (finishedNotifications.replace(notification.getNotificationId(), now()) != null) {
                log.debug("DPH - Deleting notification that has been handled or given up again: notification = {}", notification);
                handled.add(notification);
                continue;
            }
            try {
                handleNotification(onBehalfOf, herId, notification);
                finishedNotifications.put(notification.getNotificationId(), now());
                handled.add(notification);
            } catch (Exception e) {
                if (isGivenUp(notification, e)) {
                    finishedNotifications.put(notification.getNotificationId(), now());
                    handled.add(notification);
                }
            }
        }

        deleteNotifications(onBehalfOf, herId, handled);
    }

    /**
     * Logs the failure in full the first time and when giving up, so that a notification that fails on every poll does
     * not flood the log. A notification is always retried at least once, and a notification that is to be retried
     * later is not given up here, since that has been decided already.
     *
     * @return whether the retry period is over, so that the notification is given up and deleted
     */
    private boolean isGivenUp(Notification notification, Exception e) {
        boolean firstFailure = failingSince.putIfAbsent(notification.getNotificationId(), now()) == null;
        Throwable error = e instanceof RetryLaterException ? e.getCause() : e;

        if (!firstFailure && !(e instanceof RetryLaterException) && isRetryPeriodOver(notification)) {
            log.error("DPH - Handling notification failed, and the retry period of {} is over. " +
                "Giving up, the notification is deleted: notification = {}", getRetryPeriod(), notification, error);
            return true;
        }
        if (firstFailure) {
            log.warn("DPH - Handling notification failed: {} It is retried on every poll for up to {}: notification = {}",
                e.getMessage(), getRetryPeriod(), notification, error);
        } else {
            log.debug("DPH - Handling notification failed again: {} It is retried on the next poll: notification = {}",
                e.getMessage(), notification);
        }
        return false;
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

        String key = handledMessageKey(herId, messageId);
        HandledMessage handled = handledMessages.get(key);
        if (handled == null) {
            Optional<HandledMessage> received = receiveOrReject(onBehalfOf, herId, notification, messageId);
            if (received.isEmpty()) {
                return;
            }
            handled = received.get();
            handledMessages.put(key, handled);
            // The remaining steps get a retry period of their own
            failingSince.remove(notification.getNotificationId());
        } else {
            // Remembered for as long as the notification keeps coming back
            handled.lastSeenAt = now();
            log.debug("DPH message has already been handled, only the remaining steps are retried: incomingMessage = {}", handled.message);
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
                throw new RetryLaterException("The message of the notification is not found in MSH.", e);
            }
            log.warn("DPH - The message of the notification has not been found in MSH within the retry period of {}. " +
                "It has probably been received already, so the notification is deleted: notification = {}", getRetryPeriod(), notification);
            return Optional.empty();
        }
        if (failingSince.containsKey(notification.getNotificationId())) {
            log.debug("DPH message fetched again: incomingMessage={}", incomingMessage);
        } else {
            log.info("DPH message received: incomingMessage={}", incomingMessage);
        }

        if (isAlreadyHandled(incomingMessage)) {
            log.info("DPH message has already been received or rejected, and is only marked as downloaded: incomingMessage = {}", incomingMessage);
            return Optional.of(new HandledMessage(herId, incomingMessage, null, now()));
        }

        BusinessDocumentResponse response = null;
        EnqueueResult result;
        try {
            response = dphClientService.receiveBusinessDocument(onBehalfOf, incomingMessage.getId());
            result = enqueueBusinessDocument(incomingMessage, response);
        } catch (Exception e) {
            if (e instanceof DphException de && de.isNotFound()) {
                // Like the message, its business document may not be readable yet
                if (!isRetryPeriodOver(notification)) {
                    throw new RetryLaterException("The business document of the message is not found in MSH.", e);
                }
                // Rejecting the message now would answer a message we may already have received
                log.error("DPH - The business document has not been found in MSH within the retry period of {}. " +
                    "The message is not rejected, and the notification is deleted: incomingMessage = {}", getRetryPeriod(), incomingMessage, e);
                return Optional.empty();
            }
            if (!isFaultInMessage(e)) {
                // E.g. the adapter, MSH or the database is unavailable, or the integrasjonspunkt and the adapter do
                // not trust each other: rejecting the message would tell the sender that it is invalid
                if (!isRetryPeriodOver(notification)) {
                    throw new RetryLaterException("Receiving the message failed because of an error that is not in the message.", e);
                }
                log.error("DPH - Receiving the message has failed within the retry period of {} because of an error that is not in " +
                    "the message. It is neither rejected nor marked as downloaded, so it is left in MSH, and the notification is deleted. " +
                    "The message must be followed up manually: incomingMessage = {}", getRetryPeriod(), incomingMessage, e);
                return Optional.empty();
            }
            KvitteringStatusMessage error = getError(incomingMessage, e);
            HandledMessage rejected = new HandledMessage(herId, incomingMessage,
                getApplicationReceiptForRejected(incomingMessage, isApplicationReceipt(incomingMessage, response), error), now());
            rejected.rejectionError = error;
            rejected.sbd = response != null ? response.getSbd() : null;
            return Optional.of(rejected);
        } finally {
            closeAsic(response);
        }

        warnIfIdsDiffer(incomingMessage, response.getSbd());
        if (result == EnqueueResult.DUPLICATE) {
            // Another instance received the message at the same time, and answers it
            log.info("DPH message has just been received by another instance, and is only marked as downloaded: incomingMessage = {}", incomingMessage);
            return Optional.of(new HandledMessage(herId, incomingMessage, null, now()));
        }

        // The message is received, so nothing that fails from here on may reject it
        saveExternalSystemReference(incomingMessage);
        DialogmeldingKvitteringMessage applicationReceipt = getApplicationReceiptForReceived(
            incomingMessage, isApplicationReceipt(incomingMessage, response), result == EnqueueResult.EXPIRED);
        return Optional.of(new HandledMessage(herId, incomingMessage, applicationReceipt, now()));
    }

    /**
     * Registers a rejected message, sends the ApplicationReceipt, if any, and marks the message as downloaded. A step
     * that is done is not repeated if the notification comes back.
     */
    private void complete(Iso6523 onBehalfOf, HandledMessage handled) {
        if (handled.rejectionError != null && !handled.rejectionRegistered) {
            registerRejection(handled);
            handled.rejectionRegistered = true;
        }
        if (handled.pendingApplicationReceipt != null) {
            if (handled.applicationReceiptMessage == null) {
                handled.applicationReceiptMessage = nextMoveMessageService.createMessage(createApplicationReceiptSbd(handled));
            }
            nextMoveMessageService.sendMessage(handled.applicationReceiptMessage);
            handled.pendingApplicationReceipt = null;
        }
        if (!handled.downloaded) {
            markAsDownloaded(onBehalfOf, handled);
            handled.downloaded = true;
        }
    }

    /**
     * Registers the rejected message as an incoming conversation with the status FEIL, before it is answered, so that
     * it is recognized as handled if its notification comes back, also after a restart or on another instance. The
     * conversation of a message sent from this integrasjonspunkt is left alone.
     */
    private void registerRejection(HandledMessage handled) {
        IncomingMessage incomingMessage = handled.message;
        String messageId = incomingMessage.getBusinessDocumentId();
        try {
            Optional<Conversation> existing = conversationService.findConversation(messageId);
            if (existing.isPresent() && existing.get().getDirection() != ConversationDirection.INCOMING) {
                return;
            }
            // An incoming conversation exists if registering the status failed before
            if (existing.isEmpty()) {
                conversationService.registerConversation(rejectedMessage(handled));
            }
            conversationService.registerStatus(messageId, ReceiptStatus.FEIL,
                "Message was rejected when received: " + handled.rejectionError.getText());
        } catch (RuntimeException e) {
            if (isUnavailable(e)) {
                throw e;
            }
            // E.g. an id too long for the database: the sender is answered anyway
            log.warn("DPH - Registering the rejected message failed. It is answered anyway, but it may be rejected again " +
                "if its notification comes back after a restart: incomingMessage = {}", incomingMessage, e);
        }
    }

    private RejectedMessage rejectedMessage(HandledMessage handled) {
        IncomingMessage incomingMessage = handled.message;
        String messageId = incomingMessage.getBusinessDocumentId();
        StandardBusinessDocument sbd = handled.sbd;
        String defaultDocumentType = incomingMessage.isAppRec()
            ? properties.getDph().getReceiptDocumentType()
            : properties.getDph().getDialogmeldingDocumentType();
        // The business document of a rejected message may lack any of these
        return new RejectedMessage(
            sbd != null ? valueOrDefault(sbd::getConversationId, messageId) : messageId,
            messageId,
            NhnIdentifier.herId(incomingMessage.getSenderHerId()),
            NhnIdentifier.herId(handled.herId),
            sbd != null ? valueOrDefault(sbd::getProcess, properties.getDph().getNhnProcess()) : properties.getDph().getNhnProcess(),
            sbd != null ? valueOrDefault(sbd::getDocumentType, defaultDocumentType) : defaultDocumentType,
            ConversationDirection.INCOMING,
            ServiceIdentifier.DPH,
            now().plusHours(properties.getDph().getDefaultTtlHours()));
    }

    private static String valueOrDefault(Supplier<String> value, String defaultValue) {
        try {
            String v = value.get();
            return v != null ? v : defaultValue;
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    /**
     * Whether the database is unavailable, so that the step is retried instead of skipped.
     */
    private static boolean isUnavailable(Throwable e) {
        return findCause(e, TransientDataAccessException.class) != null
            || findCause(e, RecoverableDataAccessException.class) != null
            || findCause(e, DataAccessResourceFailureException.class) != null
            || findCause(e, CannotCreateTransactionException.class) != null;
    }

    /**
     * The notification of a handled message comes back if deleting it failed. A received message is put on the local
     * queue with the id of its business document. Its conversation is incoming, unless the sender is on this
     * integrasjonspunkt too: then the conversation of the sent message is reused, and gets the status
     * INNKOMMENDE_MOTTATT when the message is received. A rejected message gets an incoming conversation too.
     * <p>
     * A message to several HER-ids on this integrasjonspunkt has one business document, so it is only received once.
     */
    private boolean isAlreadyHandled(IncomingMessage incomingMessage) {
        return conversationService.findConversation(incomingMessage.getBusinessDocumentId())
            .filter(conversation -> conversation.getDirection() == ConversationDirection.INCOMING
                || hasStatus(conversation, ReceiptStatus.INNKOMMENDE_MOTTATT))
            .isPresent();
    }

    private static boolean hasStatus(Conversation conversation, ReceiptStatus status) {
        return conversation.getMessageStatuses() != null && conversation.findStatus(status) != null;
    }

    /**
     * Whether the message is an ApplicationReceipt, according to MSH or to its business document. An
     * ApplicationReceipt is never answered with another ApplicationReceipt, to avoid loops between the parties.
     */
    private static boolean isApplicationReceipt(IncomingMessage incomingMessage, BusinessDocumentResponse response) {
        return incomingMessage.isAppRec()
            || response != null && response.getSbd() != null
            && BusinessMessageType.DIALOGMELDING_KVITTERING.getType().equalsIgnoreCase(valueOrDefault(response.getSbd()::getType, null));
    }

    /**
     * @return the ApplicationReceipt to send for a received message, if any
     */
    private DialogmeldingKvitteringMessage getApplicationReceiptForReceived(IncomingMessage incomingMessage, boolean isApplicationReceipt, boolean expired) {
        if (isApplicationReceipt) {
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
     * @return why the message is rejected, as told to the sender
     */
    private static KvitteringStatusMessage getError(IncomingMessage incomingMessage, Exception e) {
        log.warn("DPH message is invalid, and is rejected: incomingMessage = {}", incomingMessage, e);
        InvalidBusinessDocumentException invalid = findCause(e, InvalidBusinessDocumentException.class);
        return new KvitteringStatusMessage()
            .setCode(invalid.getErrorCode())
            .setText(invalid.getMessage());
    }

    /**
     * @return the ApplicationReceipt that rejects the message, or nothing if the message is an ApplicationReceipt
     */
    private DialogmeldingKvitteringMessage getApplicationReceiptForRejected(IncomingMessage incomingMessage, boolean isApplicationReceipt, KvitteringStatusMessage error) {
        if (isApplicationReceipt) {
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
     * Whether the error is a fault in the message itself, so that the message is rejected at once: its business
     * document has been fetched, but cannot be deserialized, has an unknown type, or lacks its content.
     * <p>
     * Any other error is not about the message, and retrying may help: errors from the adapter, which answers 5xx
     * when MSH is unavailable or answers with data it cannot read, errors on the connection to the adapter, e.g. when
     * the integrasjonspunkt and the adapter do not trust each other's certificates, and database errors, e.g. a
     * conflict when another instance received the same message at the same time.
     */
    static boolean isFaultInMessage(Throwable e) {
        return findCause(e, InvalidBusinessDocumentException.class) != null;
    }

    /**
     * Whether the adapter refused the request, so that retrying it cannot help, unlike an authorization that is not
     * in place yet, a timeout or throttling.
     */
    private static boolean isRefusedByAdapter(DphException e) {
        Integer statusCode = e.getStatusCode();
        return statusCode != null && statusCode >= 400 && statusCode < 500 && !TEMPORARY_CLIENT_ERRORS.contains(statusCode);
    }

    private static <T extends Throwable> T findCause(Throwable e, Class<T> type) {
        Set<Throwable> seen = new HashSet<>();
        for (Throwable t = e; t != null && seen.add(t); t = t.getCause()) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
        }
        return null;
    }

    private boolean isRetryPeriodOver(Notification notification) {
        OffsetDateTime since = failingSince.get(notification.getNotificationId());
        return since != null && since.plus(getRetryPeriod()).isBefore(now());
    }

    private Duration getRetryPeriod() {
        return properties.getDph().getNotificationRetryPeriod();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private static String handledMessageKey(Integer herId, String messageId) {
        return herId + ":" + messageId;
    }

    /**
     * Marks the message as downloaded, which deletes its business document in MSH. A message that is not found has
     * already been marked as downloaded. If the adapter or MSH refuses the request, retrying cannot help, so the
     * message is left in MSH.
     */
    private void markAsDownloaded(Iso6523 onBehalfOf, HandledMessage handled) {
        try {
            dphClientService.markAsDownloaded(onBehalfOf, handled.herId, handled.message.getId());
        } catch (DphException e) {
            if (e.isNotFound()) {
                log.info("DPH message has already been marked as downloaded: incomingMessage = {}", handled.message);
            } else if (isRefusedByAdapter(e)) {
                log.warn("DPH message cannot be marked as downloaded, and is left in MSH: incomingMessage = {}", handled.message, e);
            } else {
                throw e;
            }
        }
    }

    /**
     * Deletes the notifications, and forgets what is remembered about them. A notification that cannot be deleted
     * comes back on the next poll, and is handled again from what is remembered.
     */
    private void deleteNotifications(Iso6523 onBehalfOf, Integer herId, List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }
        Exception failure = null;
        List<String> failedIds = new ArrayList<>();
        for (int from = 0; from < notifications.size(); from += MAX_NOTIFICATIONS_TO_DELETE) {
            List<Notification> chunk = notifications.subList(from, Math.min(from + MAX_NOTIFICATIONS_TO_DELETE, notifications.size()));
            List<String> notificationIds = chunk.stream().map(Notification::getNotificationId).toList();
            try {
                dphClientService.deleteNotifications(onBehalfOf, notificationIds);
            } catch (Exception e) {
                failure = failure == null ? e : failure;
                failedIds.addAll(notificationIds);
                continue;
            }
            forget(herId, chunk);
        }

        // Logged once per poll, so that a chunk that fails and one that succeeds do not log on every poll
        if (failure == null) {
            if (herIdsFailingToDeleteNotifications.remove(herId)) {
                log.info("DPH - Deleting notifications works again for HER-id = {}", herId);
            }
        } else if (herIdsFailingToDeleteNotifications.add(herId)) {
            log.error("DPH - Deleting handled notifications failed for HER-id = {}. They will come back on the next poll, " +
                "but are not handled again: notificationIds = {}", herId, failedIds, failure);
        } else {
            log.debug("DPH - Deleting handled notifications failed again for HER-id = {}: notificationIds = {}", herId, failedIds);
        }
    }

    /**
     * Forgets what is remembered about the notifications, which have been deleted.
     */
    private void forget(Integer herId, List<Notification> notifications) {
        for (Notification notification : notifications) {
            failingSince.remove(notification.getNotificationId());
            finishedNotifications.remove(notification.getNotificationId());
            // Only a NewMessage notification has handled steps, a status notification may refer to the same message
            if (notification.getType() == NotificationType.NEW_MESSAGE && notification.getRelatedMessageId() != null) {
                handledMessages.remove(handledMessageKey(herId, notification.getRelatedMessageId()));
            }
        }
    }

    /**
     * What is remembered about a notification is forgotten when it is deleted. If it was deleted some other way, it
     * no longer comes back, and is forgotten when it has not been seen for so long that it would have been given up
     * anyway.
     */
    private void forgetWhatIsTooOldToMatter() {
        Duration memoryPeriod = getRetryPeriod().multipliedBy(2);
        if (memoryPeriod.compareTo(MIN_MEMORY_PERIOD) < 0) {
            memoryPeriod = MIN_MEMORY_PERIOD;
        }
        OffsetDateTime limit = now().minus(memoryPeriod);
        handledMessages.values().removeIf(handled -> handled.lastSeenAt.isBefore(limit));
        finishedNotifications.values().removeIf(lastSeenAt -> lastSeenAt.isBefore(limit));
        failingSince.values().removeIf(since -> since.isBefore(limit));
    }

    /**
     * Saves the NHN message id on the conversation of the received message. The conversation of a sent message is
     * left alone, since its reference is used to poll its statuses. The message is received already, so a failure is
     * only logged.
     */
    private void saveExternalSystemReference(IncomingMessage incomingMessage) {
        try {
            conversationService.findConversation(incomingMessage.getBusinessDocumentId())
                .filter(conversation -> conversation.getDirection() == ConversationDirection.INCOMING)
                .ifPresent(conversation -> {
                    conversation.setExternalSystemReference(incomingMessage.getId());
                    conversationService.save(conversation);
                });
        } catch (Exception e) {
            log.warn("DPH - Saving the NHN message id on the conversation failed. The message is received anyway: incomingMessage = {}", incomingMessage, e);
        }
    }

    /**
     * A message that has been received is recognized by the id of its business document in MSH, which should be the
     * message id in the business document. If they differ, the message may be received again if its notification
     * comes back after a restart.
     */
    private static void warnIfIdsDiffer(IncomingMessage incomingMessage, StandardBusinessDocument sbd) {
        String sbdMessageId = sbd.getMessageId();
        if (!incomingMessage.getBusinessDocumentId().equals(sbdMessageId)) {
            log.warn("DPH - The business document id in MSH, {}, differs from the message id in the business document, {}. " +
                "The message cannot be recognized as received if its notification comes back after a restart: incomingMessage = {}",
                incomingMessage.getBusinessDocumentId(), sbdMessageId, incomingMessage);
        }
    }

    /**
     * Deletes the temporary file of a large ASiC, which has been copied when the message was put on the queue.
     */
    private static void closeAsic(BusinessDocumentResponse response) {
        if (response != null && response.getEncryptedAsic() instanceof AutoCloseable asic) {
            try {
                asic.close();
            } catch (Exception e) {
                log.warn("DPH - Deleting the temporary file of the ASiC failed", e);
            }
        }
    }

    private StandardBusinessDocument createApplicationReceiptSbd(HandledMessage handled) {
        return sbdFactory.createNextMoveSBD(
            NhnIdentifier.herId(handled.herId),
            NhnIdentifier.herId(handled.message.getSenderHerId()),
            handled.message.getBusinessDocumentId(),
            null,
            properties.getDph().getReceiptProcess(),
            properties.getDph().getReceiptDocumentType(),
            handled.pendingApplicationReceipt
        );
    }

    private EnqueueResult enqueueBusinessDocument(IncomingMessage incomingMessage, BusinessDocumentResponse response) {
        StandardBusinessDocument sbd = response.getSbd();

        log.debug("DPH {} received: incomingMessage={}, response={}", sbd.getType(), incomingMessage, response);

        BusinessMessageType type;
        try {
            type = BusinessMessageType.fromType(sbd.getType());
        } catch (IllegalArgumentException e) {
            throw new InvalidBusinessDocumentException(FeilmeldingForApplikasjonskvittering.IKKE_STOTTET_FORMAT, e);
        }
        return switch (type) {
            case DIALOGMELDING -> nextMoveQueue.enqueueIncomingMessage(sbd, ServiceIdentifier.DPH, response.getEncryptedAsic());
            case DIALOGMELDING_KVITTERING -> handleApplicationReceipt(sbd);
            default -> throw new InvalidBusinessDocumentException(FeilmeldingForApplikasjonskvittering.IKKE_STOTTET_FORMAT);
        };
    }

    private EnqueueResult handleApplicationReceipt(StandardBusinessDocument sbd) {
        DialogmeldingKvitteringMessage message = sbd.getBusinessMessage(DialogmeldingKvitteringMessage.class)
            .orElseThrow(() -> new InvalidBusinessDocumentException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL));

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
            return nextMoveQueue.enqueueIncomingMessage(sbd, ServiceIdentifier.DPH);
        }
        log.info("Skipping ApplicationReceipt since difi.move.feature.enable-receipts=false");
        return EnqueueResult.STATUS;
    }

    /**
     * A message that has been received or rejected, with the steps that remain before its notification can be deleted.
     */
    private static final class HandledMessage {
        private final Integer herId;
        private final IncomingMessage message;
        /**
         * When the message was handled, or its notification last came back.
         */
        private OffsetDateTime lastSeenAt;
        private DialogmeldingKvitteringMessage pendingApplicationReceipt;
        /**
         * The outgoing message of the pending ApplicationReceipt once it is created, so that it is not created again
         * if sending it fails.
         */
        private NextMoveOutMessage applicationReceiptMessage;
        private boolean downloaded;
        /**
         * Why the message was rejected, if it was.
         */
        private KvitteringStatusMessage rejectionError;
        /**
         * The business document of a rejected message, if it could be read.
         */
        private StandardBusinessDocument sbd;
        private boolean rejectionRegistered;

        private HandledMessage(Integer herId, IncomingMessage message, DialogmeldingKvitteringMessage pendingApplicationReceipt, OffsetDateTime handledAt) {
            this.herId = herId;
            this.message = message;
            this.pendingApplicationReceipt = pendingApplicationReceipt;
            this.lastSeenAt = handledAt;
        }
    }

    /**
     * The conversation of a rejected message.
     */
    @Value
    private static class RejectedMessage implements MessageInformable {
        String conversationId;
        String messageId;
        PartnerIdentifier sender;
        PartnerIdentifier receiver;
        String processIdentifier;
        String documentIdentifier;
        ConversationDirection direction;
        ServiceIdentifier serviceIdentifier;
        OffsetDateTime expiry;
    }

    /**
     * The notification cannot be handled yet, and is retried on the next poll.
     */
    private static final class RetryLaterException extends RuntimeException {
        private RetryLaterException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
