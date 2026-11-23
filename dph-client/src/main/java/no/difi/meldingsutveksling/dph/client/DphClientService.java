package no.difi.meldingsutveksling.dph.client;

import com.nimbusds.jose.JWSObject;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.dph.client.domain.BusinessDocumentResponse;
import no.difi.meldingsutveksling.dph.client.domain.SendBusinessDocumentInput;
import no.difi.meldingsutveksling.dph.client.internal.DphClient;
import no.difi.meldingsutveksling.dph.client.internal.DphParcelService;
import no.difi.meldingsutveksling.dph.client.internal.WrappedPackage;
import no.difi.meldingsutveksling.nhn.adapter.model.GetDocumentInput;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.MessageStatus;
import no.difi.meldingsutveksling.nhn.adapter.model.Notification;
import no.difi.meldingsutveksling.nhn.adapter.model.serialization.KxJson;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class DphClientService {

    private final DphClient dphClient;
    private final DphParcelService parcelService;

    public List<MessageStatus> getStatus(Iso6523 onBehalfOf, String messageId) {
        return dphClient.getStatus(onBehalfOf, messageId);
    }

    public UUID sendBusinessDocument(Iso6523 onBehalfOf, SendBusinessDocumentInput input) {
        String foretningsmelding = parcelService.signAndEncrypt(parcelService.toJSON(input.getSbd()));
        return dphClient.sendBusinessDocument(onBehalfOf, new WrappedPackage(foretningsmelding, input.getEncryptedAsic()));
    }

    /**
     * The unread notifications of {@code herId}, at most as many as the adapter's default. A notification stays
     * unread until it is deleted with {@link #deleteNotifications(Iso6523, List)}.
     */
    public List<Notification> getUnreadNotifications(Iso6523 onBehalfOf, Integer herId) {
        return dphClient.getUnreadNotifications(onBehalfOf, herId, null);
    }

    /**
     * @param notificationsToFetch at most this many notifications (1-1000)
     */
    public List<Notification> getUnreadNotifications(Iso6523 onBehalfOf, Integer herId, int notificationsToFetch) {
        return dphClient.getUnreadNotifications(onBehalfOf, herId, notificationsToFetch);
    }

    public void deleteNotifications(Iso6523 onBehalfOf, List<String> notificationIds) {
        dphClient.deleteNotifications(onBehalfOf, notificationIds);
    }

    public IncomingMessage getMessage(Iso6523 onBehalfOf, Integer receiverHerId, String messageId) {
        return dphClient.getMessage(onBehalfOf, receiverHerId, messageId);
    }

    public BusinessDocumentResponse receiveBusinessDocument(Iso6523 onBehalfOf, String id) {
        String jweToken = parcelService.signAndEncrypt(KxJson.encode(new GetDocumentInput(id), GetDocumentInput.Companion.serializer()));
        WrappedPackage wrappedPackage = dphClient.receiveBusinessDocument(onBehalfOf, jweToken);
        JWSObject jws = parcelService.decryptAndVerify(wrappedPackage.forretningsmelding());
        String json = jws.getPayload().toString();

        return new BusinessDocumentResponse()
            .setSbd(parcelService.toSBD(json))
            .setEncryptedAsic(wrappedPackage.encryptedAsic());
    }

    /**
     * Marks the message as downloaded, which deletes its business document in MSH. Its notification must be deleted
     * separately.
     */
    public void markAsDownloaded(Iso6523 onBehalfOf, Integer receiverHerId, String messageId) {
        dphClient.markAsDownloaded(onBehalfOf, receiverHerId, messageId);
    }

    public String getMaskinportenToken(Iso6523 onBehalfOf) {
        return dphClient.getMaskinportenToken(onBehalfOf);
    }
}
