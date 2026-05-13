package no.difi.meldingsutveksling.dph.client;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.dph.client.domain.ApplicationReceiptResponse;
import no.difi.meldingsutveksling.dph.client.domain.BusinessDocumentResponse;
import no.difi.meldingsutveksling.dph.client.domain.SendApplicationReceiptInput;
import no.difi.meldingsutveksling.dph.client.domain.SendBusinessDocumentInput;
import no.difi.meldingsutveksling.dph.client.internal.CreateMaskinportenToken;
import no.difi.meldingsutveksling.dph.client.internal.CreateMultipart;
import no.difi.meldingsutveksling.dph.client.internal.DphDocumentConverter;
import no.difi.meldingsutveksling.dph.client.internal.DphParcelService;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingApplicationReceipt;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingBusinessDocument;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.MessageStatus;
import no.difi.meldingsutveksling.nhn.adapter.model.MultipartNames;
import no.difi.meldingsutveksling.nhn.adapter.model.OutgoingApplicationReceipt;
import no.difi.meldingsutveksling.nhn.adapter.model.OutgoingBusinessDocument;
import no.difi.meldingsutveksling.nhn.adapter.model.serialization.KxJson;
import no.difi.move.common.dokumentpakking.PartUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static kotlinx.serialization.builtins.BuiltinSerializersKt.ListSerializer;
import static no.difi.meldingsutveksling.dph.client.internal.CreateMultipart.APPLICATION_JOSE;

@RequiredArgsConstructor
public class DphClientImpl implements DphClient {

    private final WebClient webClient;
    private final CreateMultipart createMultipart;
    private final DphParcelService parcelService;
    private final DphDocumentConverter dphDocumentConverter;
    private final DphClientErrorHandler errorHandler;
    private final CreateMaskinportenToken createMaskinportenToken;

    @Override
    public List<MessageStatus> getStatus(Iso6523 onBehalfOf, String messageId) {
        return webClient.get()
            .uri("/messages/out/{messageId}/statuses", messageId)
            .headers(h -> h.setBearerAuth(getMaskinportenToken(onBehalfOf)))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler)
            .bodyToMono(String.class)
            .map(json -> KxJson.decode(json, ListSerializer(MessageStatus.Companion.serializer())))
            .block();
    }

    @Override
    public UUID sendBusinessDocument(Iso6523 onBehalfOf, SendBusinessDocumentInput input) {
        String foretningsmelding = parcelService.signAndEncrypt(KxJson.encode(
            dphDocumentConverter.toExternal(input), OutgoingBusinessDocument.Companion.serializer()
        ));

        return webClient.post()
            .uri("/messages/out")
            .headers(h -> h.setBearerAuth(getMaskinportenToken(onBehalfOf)))
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromMultipartData(createMultipart.createMultipart(foretningsmelding, input.getEncryptedAsic())))
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler)
            .bodyToMono(String.class)
            .map(UUID::fromString)
            .block();
    }

    @Override
    public UUID sendApplicationReceipt(Iso6523 onBehalfOf, SendApplicationReceiptInput input) {
        String foretningsmelding = parcelService.signAndEncrypt(KxJson.encode(
            dphDocumentConverter.toExternal(input),
            OutgoingApplicationReceipt.Companion.serializer()));

        return webClient.post()
            .uri("/messages/out/receipt")
            .headers(h -> h.setBearerAuth(getMaskinportenToken(onBehalfOf)))
            .contentType(APPLICATION_JOSE)
            .accept(MediaType.TEXT_PLAIN)
            .bodyValue(foretningsmelding)
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler)
            .bodyToMono(String.class)
            .map(UUID::fromString)
            .block();
    }


    @Override
    public List<IncomingMessage> getMessages(Iso6523 onBehalfOf, Integer receiverHerId) {
        return webClient.get()
            .uri("/messages/in", builder -> builder
                .queryParam("receiverHerId", receiverHerId)
                .build())
            .headers(h -> h.setBearerAuth(getMaskinportenToken(onBehalfOf)))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler)
            .bodyToMono(String.class)
            .map(json -> KxJson.decode(json, ListSerializer(IncomingMessage.Companion.serializer())))
            .block();
    }

    @Override
    public ApplicationReceiptResponse receiveApplicationReceipt(Iso6523 onBehalfOf, String id) {
        MultiValueMap<String, Part> parts = Optional.ofNullable(webClient.get()
            .uri("/messages/in/{id}/receipt", id)
            .headers(h -> h.setBearerAuth(getMaskinportenToken(onBehalfOf)))
            .accept(MediaType.MULTIPART_MIXED)
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler)
            .bodyToMono(new ParameterizedTypeReference<MultiValueMap<String, Part>>() {
            })
            .block()).orElseThrow(() -> new IllegalStateException("Could not receive application receipt for id " + id));

        IncomingApplicationReceipt businessDocument = getIncomingApplicationReceipt(getPart(parts, MultipartNames.FORRETNINGSMELDING));
        return new ApplicationReceiptResponse()
            .setMessageId(businessDocument.getId())
            .setRawReceipt(businessDocument.getRawReceipt())
            .setPayload(dphDocumentConverter.toInternal(businessDocument.getPayload()))
            .setEncryptedAsic(parcelService.getEncryptedAsic(getPart(parts, MultipartNames.DOKUMENTPAKKE)));
    }

    private IncomingApplicationReceipt getIncomingApplicationReceipt(Part part) {
        String jweToken = PartUtils.toString(part);
        String json = parcelService.decryptAndVerify(jweToken);
        return KxJson.decode(json, IncomingApplicationReceipt.Companion.serializer());
    }

    @Override
    public BusinessDocumentResponse receiveBusinessDocument(Iso6523 onBehalfOf, String id) {
        MultiValueMap<String, Part> parts = Optional.ofNullable(webClient.get()
            .uri("/messages/in/{id}", id)
            .headers(h -> h.setBearerAuth(getMaskinportenToken(onBehalfOf)))
            .accept(MediaType.MULTIPART_MIXED)
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler)
            .bodyToMono(new ParameterizedTypeReference<MultiValueMap<String, Part>>() {
            })
            .block()).orElseThrow(() -> new DphException("Could not receive business document for id " + id));

        IncomingBusinessDocument businessDocument = getIncomingBusinessDocument(getPart(parts, MultipartNames.FORRETNINGSMELDING));

        return new BusinessDocumentResponse()
            .setMessageId(businessDocument.getId())
            .setSenderHerId(businessDocument.getSenderHerId())
            .setReceiverHerId(businessDocument.getReceiverHerId())
            .setConversationId(businessDocument.getConversationId())
            .setParentId(businessDocument.getParentId())
            .setPayload(dphDocumentConverter.toInternal(businessDocument.getPayload()))
            .setEncryptedAsic(parcelService.getEncryptedAsic(getPart(parts, MultipartNames.DOKUMENTPAKKE)));
    }

    private IncomingBusinessDocument getIncomingBusinessDocument(Part part) {
        String jweToken = PartUtils.toString(part);
        String json = parcelService.decryptAndVerify(jweToken);
        return KxJson.decode(json, IncomingBusinessDocument.Companion.serializer());
    }

    @Override
    public void markAsRead(Iso6523 onBehalfOf, Integer receiverHerId, String messageId) {
        webClient.post()
            .uri("/messages/in/{messageId}/read", builder -> builder
                .queryParam("receiverHerId", receiverHerId)
                .build(messageId))
            .headers(h -> h.setBearerAuth(getMaskinportenToken(onBehalfOf)))
            .accept(MediaType.parseMediaType("application/jose"))
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler)
            .toBodilessEntity()
            .block();
    }

    @Override
    public String getMaskinportenToken(Iso6523 onBehalfOf) {
        return createMaskinportenToken.createMaskinportenToken(onBehalfOf);
    }

    private static @NonNull Part getPart(MultiValueMap<String, Part> parts, String key) {
        return Optional.ofNullable(parts.getFirst(key))
            .orElseThrow(() -> new DphException("%s part not found in response".formatted(key)));
    }
}
