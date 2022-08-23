package no.difi.meldingsutveksling.nextmove.servicebus;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.nextmove.BrokerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@RequiredArgsConstructor
public class ServiceBusRestClient {

    private static final String AUTH_HEADER = "Authorization";

    private final IntegrasjonspunktProperties props;
    private final ObjectMapper objectMapper;
    private final ServiceBusPayloadConverter payloadConverter;
    private final NextMoveQueue nextMoveQueue;
    private final ServiceBusUtil serviceBusUtil;
    private final ServiceBusRestTemplate restTemplate;

    public String getBase() {
        return format("%s://%s", props.getNextmove().getServiceBus().isUseHttps() ? "https" : "http",
                props.getNextmove().getServiceBus().getBaseUrl());
    }

    public void sendMessage(byte[] message, String queuePath) {
        String resourceUri = format("%s/%s/messages",
                getBase(),
                queuePath);
        URI uri = convertToUri(resourceUri);

        String auth = createAuthorizationHeader(resourceUri);
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTH_HEADER, auth);
        headers.add("BrokerProperties", "{}");
        HttpEntity<Object> httpEntity = new HttpEntity<>(message, headers);

        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, httpEntity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("{} got response {}, error sending message to service bus", resourceUri, response.getStatusCode());
        }
    }

    public Optional<ServiceBusMessage> receiveMessage() {
        String resourceUri = format("%s/%s/messages/head", getBase(), serviceBusUtil.getLocalQueuePath());

        String auth = createAuthorizationHeader(resourceUri);
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTH_HEADER, auth);
        HttpEntity<Object> httpEntity = new HttpEntity<>(headers);

        URI uri = convertToUri(resourceUri);
        log.debug("Calling {}", resourceUri);
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, httpEntity, String.class);
            if (!asList(HttpStatus.OK, HttpStatus.CREATED).contains(response.getStatusCode())) {
                log.debug("{} got response {}, returning empty", resourceUri, response.getStatusCode());
                return Optional.empty();
            }

            ServiceBusMessage.ServiceBusMessageBuilder sbmBuilder = ServiceBusMessage.builder();
            BrokerProperties brokerProperties = getBrokerProperties(response);

            String messageId = brokerProperties.getMessageId();
            sbmBuilder.lockToken(brokerProperties.getLockToken())
                    .messageId(messageId)
                    .sequenceNumber(brokerProperties.getSequenceNumber());

            try {
                ServiceBusPayload payload = payloadConverter.convert(Objects.requireNonNull(response.getBody()));
                sbmBuilder.payload(payload);
                log.debug(format("Received message on queue=%s with messageId=%s", serviceBusUtil.getLocalQueuePath(),
                        payload.getSbd().getMessageId()));
                return Optional.of(sbmBuilder.build());
            } catch (IOException e) {
                log.error(String.format("Error extracting ServiceBusPayload from message id=%s", messageId), e);
            }
        } catch (ResourceAccessException | IOException e) {
            log.error("Polling of DPE messages failed with: {}", e.getLocalizedMessage());
            return Optional.empty();
        }
        return Optional.empty();
    }

    public void getAllMessagesRest() {
        log.debug("Checking for new DPE messages with REST client..");

        boolean messagesInQueue = true;
        while (messagesInQueue) {
            ArrayList<ServiceBusMessage> messages = new ArrayList<>();
            for (int i = 0; i < props.getNextmove().getServiceBus().getReadMaxMessages(); i++) {
                Optional<ServiceBusMessage> msg = receiveMessage();
                if (!msg.isPresent()) {
                    messagesInQueue = false;
                    break;
                }
                messages.add(msg.get());
            }

            for (ServiceBusMessage msg : messages) {
                ByteArrayResource asicResource = (msg.getPayload().getAsic() != null)
                        ? new ByteArrayResource(Base64.getDecoder().decode(msg.getPayload().getAsic()))
                        : null;
                nextMoveQueue.enqueueIncomingMessage(msg.getPayload().getSbd(), DPE, asicResource);
                deleteMessage(msg);
            }
        }
    }

    private BrokerProperties getBrokerProperties(ResponseEntity<String> response) throws IOException {
        String brokerPropertiesJson = response.getHeaders().getFirst("BrokerProperties");
        return objectMapper.readValue(brokerPropertiesJson, BrokerProperties.class);
    }

    public void deleteMessage(ServiceBusMessage message) {
        String resourceUri = format("%s/%s/messages/%s/%s",
                getBase(),
                serviceBusUtil.getLocalQueuePath(),
                message.getMessageId(),
                message.getLockToken());

        String auth = createAuthorizationHeader(resourceUri);
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTH_HEADER, auth);
        HttpEntity<Object> httpEntity = new HttpEntity<>(headers);

        URI uri = convertToUri(resourceUri);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, httpEntity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("{} got response {}, message [messageId={}] was not deleted",
                    resourceUri, response.getStatusCode(),
                    message.getMessageId());
        }
    }

    private URI convertToUri(String uriString) {
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            throw new MeldingsUtvekslingRuntimeException(format("Uri syntax error in: %s", uriString), e);
        }
    }

    private String createAuthorizationHeader(String resourceUri) {

        String urlEncoded;
        try {
            urlEncoded = URLEncoder.encode(resourceUri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }

        int expiry = Math.round(Instant.now().plusSeconds(20).toEpochMilli() / 1000f);
        String hashInput = urlEncoded + "\n" + expiry;
        byte[] bytes = hmacSha256(serviceBusUtil.getSasKey(), hashInput);
        byte[] signEncoded = Base64.getEncoder().encode(bytes);
        String signature;
        try {
            signature = URLEncoder.encode(new String(signEncoded), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }

        return format("SharedAccessSignature sr=%s&sig=%s&se=%s&skn=%s",
                urlEncoded,
                signature,
                expiry,
                props.getNextmove().getServiceBus().getSasKeyName());
    }

    private byte[] hmacSha256(String secret, String message) {
        try {
            Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSHA256.init(secretKeySpec);
            return hmacSHA256.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

}
