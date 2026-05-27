package no.difi.meldingsutveksling.dph.client.internal;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.dph.client.DphException;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.MessageStatus;
import no.difi.meldingsutveksling.nhn.adapter.model.MultipartNames;
import no.difi.meldingsutveksling.nhn.adapter.model.serialization.KxJson;
import no.difi.move.common.dokumentpakking.PartUtils;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;
import org.jspecify.annotations.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;
import reactor.util.retry.RetrySpec;

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
    private final DphClientErrorHandler errorHandler;
    private final CreateMaskinportenToken createMaskinportenToken;
    private final RetrySpec retrySpec = Retry.max(3).filter(e -> {
        if (e instanceof DphException de) {
            return de.getStatusCode() != null && de.getStatusCode() >= 500;
        }
        return false;
    });

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
            .retryWhen(retrySpec)
            .block();
    }

    @Override
    public UUID sendBusinessDocument(Iso6523 onBehalfOf, WrappedPackage wrappedPackage) {
        return webClient.post()
            .uri("/messages/out")
            .headers(h -> h.setBearerAuth(getMaskinportenToken(onBehalfOf)))
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromMultipartData(createMultipart.createMultipart(wrappedPackage)))
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler)
            .bodyToMono(String.class)
            .map(UUID::fromString)
            .block();
    }

    @Override
    public UUID sendApplicationReceipt(Iso6523 onBehalfOf, WrappedPackage wrappedPackage) {
        return webClient.post()
            .uri("/messages/out/receipt")
            .headers(h -> h.setBearerAuth(getMaskinportenToken(onBehalfOf)))
            .contentType(APPLICATION_JOSE)
            .accept(MediaType.TEXT_PLAIN)
            .bodyValue(wrappedPackage.forretningsmelding())
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
            .retryWhen(retrySpec)
            .block();
    }

    @Override
    public WrappedPackage receiveApplicationReceipt(Iso6523 onBehalfOf, String id) {
        MultiValueMap<String, Part> parts = Optional.ofNullable(webClient.get()
            .uri("/messages/in/{id}/receipt", id)
            .headers(h -> h.setBearerAuth(getMaskinportenToken(onBehalfOf)))
            .accept(MediaType.MULTIPART_MIXED)
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler)
            .bodyToMono(new ParameterizedTypeReference<MultiValueMap<String, Part>>() {
            })
            .retryWhen(retrySpec)
            .block()).orElseThrow(() -> new DphException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL));

        return new WrappedPackage(PartUtils.toString(getPart(parts, MultipartNames.FORRETNINGSMELDING)),
            parcelService.getEncryptedAsic(getPart(parts, MultipartNames.DOKUMENTPAKKE))
        );
    }

    @Override
    public WrappedPackage receiveBusinessDocument(Iso6523 onBehalfOf, String id) {
        MultiValueMap<String, Part> parts = Optional.ofNullable(webClient.get()
            .uri("/messages/in/{id}", id)
            .headers(h -> h.setBearerAuth(getMaskinportenToken(onBehalfOf)))
            .accept(MediaType.MULTIPART_MIXED)
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler)
            .bodyToMono(new ParameterizedTypeReference<MultiValueMap<String, Part>>() {
            })
            .retryWhen(retrySpec)
            .block()).orElseThrow(() -> new DphException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL));

        return new WrappedPackage(PartUtils.toString(getPart(parts, MultipartNames.FORRETNINGSMELDING)),
            parcelService.getEncryptedAsic(getPart(parts, MultipartNames.DOKUMENTPAKKE))
        );
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
            .retryWhen(retrySpec)
            .block();
    }

    @Override
    public String getMaskinportenToken(Iso6523 onBehalfOf) {
        return createMaskinportenToken.createMaskinportenToken(onBehalfOf);
    }

    private static @NonNull Part getPart(MultiValueMap<String, Part> parts, String key) {
        return Optional.ofNullable(parts.getFirst(key))
            .orElseThrow(() -> new DphException(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL));
    }
}
