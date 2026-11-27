package no.difi.meldingsutveksling.dph.client.internal;

import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.MessageStatus;
import no.difi.meldingsutveksling.nhn.adapter.model.Notification;

import java.util.List;
import java.util.UUID;

public interface DphClient {

    List<MessageStatus> getStatus(Iso6523 onBehalfOf, String messageId);

    UUID sendBusinessDocument(Iso6523 onBehalfOf, WrappedPackage wrappedPackage);

    /**
     * The unread notifications of {@code herId}. A notification stays unread until it is deleted with
     * {@link #deleteNotifications(Iso6523, List)}.
     *
     * @param notificationsToFetch at most this many notifications (1-1000), or {@code null} for the adapter's default
     */
    List<Notification> getUnreadNotifications(Iso6523 onBehalfOf, Integer herId, Integer notificationsToFetch);

    void deleteNotifications(Iso6523 onBehalfOf, List<String> notificationIds);

    /**
     * The metadata of a message to {@code receiverHerId}, as referred to by a NewMessage notification.
     */
    IncomingMessage getMessage(Iso6523 onBehalfOf, Integer receiverHerId, String messageId);

    WrappedPackage receiveBusinessDocument(Iso6523 onBehalfOf, String jweToken);

    /**
     * Marks the message as downloaded by {@code receiverHerId}, which deletes its business document in MSH. The
     * notification of the message is not deleted.
     */
    void markAsDownloaded(Iso6523 onBehalfOf, Integer receiverHerId, String messageId);

    String getMaskinportenToken(Iso6523 onBehalfOf);
}
