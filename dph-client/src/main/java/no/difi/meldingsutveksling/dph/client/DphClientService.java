package no.difi.meldingsutveksling.dph.client;

import com.nimbusds.jose.JWSObject;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.dph.client.domain.ApplicationReceiptResponse;
import no.difi.meldingsutveksling.dph.client.domain.BusinessDocumentResponse;
import no.difi.meldingsutveksling.dph.client.domain.SendApplicationReceiptInput;
import no.difi.meldingsutveksling.dph.client.domain.SendBusinessDocumentInput;
import no.difi.meldingsutveksling.dph.client.internal.DphClient;
import no.difi.meldingsutveksling.dph.client.internal.DphDocumentConverter;
import no.difi.meldingsutveksling.dph.client.internal.DphParcelService;
import no.difi.meldingsutveksling.dph.client.internal.WrappedPackage;
import no.difi.meldingsutveksling.nhn.adapter.model.GetDocumentInput;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingApplicationReceipt;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingBusinessDocument;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.MessageStatus;
import no.difi.meldingsutveksling.nhn.adapter.model.OutgoingApplicationReceipt;
import no.difi.meldingsutveksling.nhn.adapter.model.OutgoingBusinessDocument;
import no.difi.meldingsutveksling.nhn.adapter.model.serialization.KxJson;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class DphClientService {

    private final DphClient dphClient;
    private final DphParcelService parcelService;
    private final DphDocumentConverter dphDocumentConverter;

    public List<MessageStatus> getStatus(Iso6523 onBehalfOf, String messageId) {
        return dphClient.getStatus(onBehalfOf, messageId);
    }

    public UUID sendBusinessDocument(Iso6523 onBehalfOf, SendBusinessDocumentInput input) {
        String foretningsmelding = parcelService.signAndEncrypt(KxJson.encode(
            dphDocumentConverter.toExternal(input), OutgoingBusinessDocument.Companion.serializer()
        ));

        return dphClient.sendBusinessDocument(onBehalfOf, new WrappedPackage(foretningsmelding, input.getEncryptedAsic()));
    }

    public UUID sendApplicationReceipt(Iso6523 onBehalfOf, SendApplicationReceiptInput input) {
        String foretningsmelding = parcelService.signAndEncrypt(KxJson.encode(
            dphDocumentConverter.toExternal(input),
            OutgoingApplicationReceipt.Companion.serializer()));

        return dphClient.sendApplicationReceipt(onBehalfOf, new WrappedPackage(foretningsmelding));
    }

    public List<IncomingMessage> getMessages(Iso6523 onBehalfOf, Integer receiverHerId) {
        return dphClient.getMessages(onBehalfOf, receiverHerId);
    }

    public ApplicationReceiptResponse receiveApplicationReceipt(Iso6523 onBehalfOf, String id) {
        String jweToken = parcelService.signAndEncrypt(KxJson.encode(new GetDocumentInput(id), GetDocumentInput.Companion.serializer()));
        WrappedPackage wrappedPackage = dphClient.receiveApplicationReceipt(onBehalfOf, jweToken);
        JWSObject jws = parcelService.decryptAndVerify(wrappedPackage.forretningsmelding());
        String json = jws.getPayload().toString();
        IncomingApplicationReceipt businessDocument = KxJson.decode(json, IncomingApplicationReceipt.Companion.serializer());

        return new ApplicationReceiptResponse()
            .setMessageId(businessDocument.getId())
            .setRawReceipt(businessDocument.getRawReceipt())
            .setPayload(dphDocumentConverter.toInternal(businessDocument.getPayload()))
            .setEncryptedAsic(wrappedPackage.encryptedAsic());
    }

    public BusinessDocumentResponse receiveBusinessDocument(Iso6523 onBehalfOf, String id) {
        String jweToken = parcelService.signAndEncrypt(KxJson.encode(new GetDocumentInput(id), GetDocumentInput.Companion.serializer()));
        WrappedPackage wrappedPackage = dphClient.receiveBusinessDocument(onBehalfOf, jweToken);
        JWSObject jws = parcelService.decryptAndVerify(wrappedPackage.forretningsmelding());
        String json = jws.getPayload().toString();
        IncomingBusinessDocument businessDocument = KxJson.decode(json, IncomingBusinessDocument.Companion.serializer());

        return new BusinessDocumentResponse()
            .setMessageId(businessDocument.getId())
            .setSenderHerId(businessDocument.getSenderHerId())
            .setReceiverHerId(businessDocument.getReceiverHerId())
            .setConversationId(businessDocument.getConversationId())
            .setParentId(businessDocument.getParentId())
            .setPayload(dphDocumentConverter.toInternal(businessDocument.getPayload()))
            .setEncryptedAsic(wrappedPackage.encryptedAsic());
    }

    public void markAsRead(Iso6523 onBehalfOf, Integer receiverHerId, String messageId) {
        dphClient.markAsRead(onBehalfOf, receiverHerId, messageId);
    }

    public String getMaskinportenToken(Iso6523 onBehalfOf) {
        return dphClient.getMaskinportenToken(onBehalfOf);
    }
}
