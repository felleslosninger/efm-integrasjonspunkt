package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dpi.client.Blame;
import no.difi.meldingsutveksling.dpi.client.DpiException;
import no.difi.meldingsutveksling.dpi.client.domain.GetMessagesInput;
import no.difi.meldingsutveksling.dpi.client.domain.Message;
import no.difi.meldingsutveksling.dpi.client.domain.MessageStatus;
import no.difi.meldingsutveksling.dpi.client.internal.domain.SendMessageInput;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class Corner2ClientImpl implements Corner2Client {

    private final WebClient webClient;
    private final DpiClientErrorHandler dpiClientErrorHandler;
    private final CreateMaskinportenToken createMaskinportenToken;
    private final CreateMultipart createMultipart;
    private final InMemoryWithTempFileFallbackResourceFactory resourceFactory;

    @Override
    public void sendMessage(SendMessageInput input) {
        webClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path("/messages/out");
                    Optional.ofNullable(input.getChannel()).ifPresent(p -> uriBuilder.queryParam("kanal", p));
                    return uriBuilder.build();
                })
                .headers(h -> h.setBearerAuth(input.getMaskinportentoken()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(createMultipart.createMultipart(input)))
                .retrieve()
                .onStatus(HttpStatus::isError, this.dpiClientErrorHandler)
                .toBodilessEntity()
                .block();
    }

    @Override
    public Flux<MessageStatus> getMessageStatuses(UUID messageId) {
        return webClient.get()
                .uri("/messages/out/{messageId}/statuses", messageId)
                .headers(h -> h.setBearerAuth(createMaskinportenToken.createMaskinportenTokenForReceiving()))
                .retrieve()
                .onStatus(HttpStatus::isError, this.dpiClientErrorHandler)
                .bodyToFlux(MessageStatus.class);
    }

    @Override
    public Flux<Message> getMessages(GetMessagesInput input) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/messages/in");
                    Optional.ofNullable(input.getSenderId()).ifPresent(p -> uriBuilder.queryParam("avsenderidentifikator", p));
                    Optional.ofNullable(input.getChannel()).ifPresent(p -> uriBuilder.queryParam("kanal", p));
                    return uriBuilder.build();
                })
                .headers(h -> h.setBearerAuth(createMaskinportenToken.createMaskinportenTokenForReceiving()))
                .retrieve()
                .onStatus(HttpStatus::isError, this.dpiClientErrorHandler)
                .bodyToFlux(Message.class);
    }

    @Override
    public InMemoryWithTempFileFallbackResource getCmsEncryptedAsice(URI downloadurl) throws DpiException {
        InMemoryWithTempFileFallbackResource cms = resourceFactory.getResource("dpi-", ".asic.cms");
        Flux<DataBuffer> dataBuffer = webClient.get()
                .uri(downloadurl)
                .headers(h -> h.setBearerAuth(createMaskinportenToken.createMaskinportenTokenForReceiving()))
                .retrieve()
                .onStatus(HttpStatus::isError, this.dpiClientErrorHandler)
                .bodyToFlux(DataBuffer.class);

        try (OutputStream outputStream = cms.getOutputStream()) {
            DataBufferUtils.write(dataBuffer, outputStream)
                    .share().blockLast();
        } catch (IOException e) {
            throw new DpiException(
                    String.format("Downloading CMS encrypted archive failed for URL: %s", downloadurl),
                    e,
                    Blame.CLIENT);
        }

        return cms;
    }

    @Override
    public void markAsRead(UUID messageId) {
        webClient.post()
                .uri("/messages/in/{messageId}/read", messageId)
                .headers(h -> h.setBearerAuth(createMaskinportenToken.createMaskinportenTokenForReceiving()))
                .retrieve()
                .onStatus(httpStatus -> httpStatus.is4xxClientError() && httpStatus != HttpStatus.NOT_FOUND, ClientResponse::createException)
                .onStatus(HttpStatus::is5xxServerError, this.dpiClientErrorHandler)
                .toBodilessEntity()
                .onErrorResume(WebClientResponseException.NotFound.class, notFound -> Mono.empty())
                .block();
    }
}
