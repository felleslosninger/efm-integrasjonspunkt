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

    public List<IncomingMessage> getMessages(Iso6523 onBehalfOf, Integer receiverHerId) {
        return dphClient.getMessages(onBehalfOf, receiverHerId);
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

    public void markAsRead(Iso6523 onBehalfOf, Integer receiverHerId, String messageId) {
        dphClient.markAsRead(onBehalfOf, receiverHerId, messageId);
    }

    public String getMaskinportenToken(Iso6523 onBehalfOf) {
        return dphClient.getMaskinportenToken(onBehalfOf);
    }
}
