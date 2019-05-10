package no.difi.meldingsutveksling.nextmove;

import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayload;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayloadConverter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
public class ServiceBusRestClient {

    private static final String NEXTMOVE_QUEUE_PREFIX = "nextbestqueue";
    private static final String AUTH_HEADER = "Authorization";

    private final ServiceRegistryLookup sr;
    private final IntegrasjonspunktProperties props;
    private final ServiceBusPayloadConverter payloadConverter;
    @Getter
    private final ServiceBusRestTemplate restTemplate;
    @Getter
    private final String localQueuePath;

    public ServiceBusRestClient(ServiceRegistryLookup sr, IntegrasjonspunktProperties props, ServiceBusPayloadConverter payloadConverter, ServiceBusRestTemplate restTemplate) {
        this.sr = sr;
        this.props = props;
        this.payloadConverter = payloadConverter;
        this.restTemplate = restTemplate;
        this.localQueuePath = NEXTMOVE_QUEUE_PREFIX +
                props.getOrg().getNumber() +
                props.getNextmove().getServiceBus().getMode();
    }

    public String getBase() {
        return format("https://%s.%s",
                props.getNextmove().getServiceBus().getNamespace(),
                props.getNextmove().getServiceBus().getHost());
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
            log.error("{} got response {}, error sending message to service bus", resourceUri, response.getStatusCode().toString());
        }
    }

    public Optional<ServiceBusMessage> receiveMessage() {
        String resourceUri = format("%s/%s/messages/head",
                getBase(),
                localQueuePath);

        String auth = createAuthorizationHeader(resourceUri);
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTH_HEADER, auth);
        HttpEntity<Object> httpEntity = new HttpEntity<>(headers);

        URI uri = convertToUri(resourceUri);
        log.debug("Calling {}", resourceUri);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, httpEntity, String.class);
        if (!asList(HttpStatus.OK, HttpStatus.CREATED).contains(response.getStatusCode())) {
            log.debug("{} got response {}, returning empty", resourceUri, response.getStatusCode().toString());
            return Optional.empty();
        }

        ServiceBusMessage.ServiceBusMessageBuilder sbmBuilder = ServiceBusMessage.builder();
        String brokerPropertiesJson = response.getHeaders().getFirst("BrokerProperties");
        JsonParser jsonParser = new JsonParser();
        JsonObject brokerProperties = jsonParser.parse(brokerPropertiesJson).getAsJsonObject();
        String messageId = brokerProperties.get("MessageId").getAsString();
        sbmBuilder.lockToken(brokerProperties.get("LockToken").getAsString())
                .messageId(messageId)
                .sequenceNumber(brokerProperties.get("SequenceNumber").getAsString());

        try {
            ServiceBusPayload payload = payloadConverter.convert(response.getBody(), messageId);
            sbmBuilder.payload(payload);
            log.debug(format("Received message on queue=%s with conversationId=%s", localQueuePath, payload.getSbd().getConversationId()));
            return Optional.of(sbmBuilder.build());
        } catch (JAXBException e) {
            log.error(String.format("Error creating old format from message id=%s", messageId), e);
        }

        return Optional.empty();
    }

    public void deleteMessage(ServiceBusMessage message) {
        String resourceUri = format("%s/%s/messages/%s/%s",
                getBase(),
                localQueuePath,
                message.getMessageId(),
                message.getLockToken());

        String auth = createAuthorizationHeader(resourceUri);
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTH_HEADER, auth);
        HttpEntity<Object> httpEntity = new HttpEntity<>(headers);

        URI uri = convertToUri(resourceUri);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.DELETE, httpEntity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("{} got response {}, message [conversationId={}] was not deleted",
                    resourceUri, response.getStatusCode().toString(),
                    message.getPayload().getSbd().getConversationId());
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

        byte[] bytes = Hashing.hmacSha256(getSasKey().getBytes(StandardCharsets.UTF_8))
                .hashBytes(hashInput.getBytes(StandardCharsets.UTF_8)).asBytes();
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

    public String getSasKey() {
        if (props.getOidc().isEnable()) {
            return sr.getSasKey();
        } else {
            return props.getNextmove().getServiceBus().getSasToken();
        }
    }
}
