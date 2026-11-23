package no.difi.meldingsutveksling.dph.client;

import com.nimbusds.jose.JWSObject;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
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
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;
import org.springframework.core.io.Resource;

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
     * The unread notifications of {@code herId}. A notification stays unread until it is deleted with
     * {@link #deleteNotifications(Iso6523, List)}.
     *
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

    /**
     * The business document of the message, with its encrypted ASiC, if any. The caller closes the ASiC, which may be
     * backed by a temporary file. If the business document cannot be read, the ASiC is closed here.
     *
     * @throws DphException without status code if the response from the adapter cannot be decrypted, verified or
     *                      deserialized, which is a fault between the integrasjonspunkt and the adapter, not in the
     *                      message: the adapter writes the business document
     */
    public BusinessDocumentResponse receiveBusinessDocument(Iso6523 onBehalfOf, String id) {
        String jweToken = parcelService.signAndEncrypt(KxJson.encode(new GetDocumentInput(id), GetDocumentInput.Companion.serializer()));
        WrappedPackage wrappedPackage = dphClient.receiveBusinessDocument(onBehalfOf, jweToken);
        try {
            JWSObject jws = parcelService.decryptAndVerify(wrappedPackage.forretningsmelding());
            String json = jws.getPayload().toString();

            return new BusinessDocumentResponse()
                .setSbd(toSBD(json))
                .setEncryptedAsic(wrappedPackage.encryptedAsic());
        } catch (RuntimeException e) {
            close(wrappedPackage.encryptedAsic(), e);
            throw e;
        }
    }

    private StandardBusinessDocument toSBD(String json) {
        try {
            return parcelService.toSBD(json);
        } catch (IllegalArgumentException e) {
            // The adapter writes the JSON, not the sender: the integrasjonspunkt and the adapter disagree on the model
            throw new DphException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL, e);
        }
    }

    private static void close(Resource asic, RuntimeException failure) {
        if (asic instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                failure.addSuppressed(e);
            }
        }
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
