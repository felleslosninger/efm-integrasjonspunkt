package no.difi.meldingsutveksling.nextmove.servicebus;

import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE;

@Slf4j
@Component
@ConditionalOnExpression("${difi.move.feature.enableDPE} && ${difi.move.nextmove.serviceBus.batchRead}")
@RequiredArgsConstructor
@DependsOn("objectMapperHolder")
public class ServiceBusAmqpClient {

    private final IntegrasjonspunktProperties properties;
    private final ServiceBusUtil serviceBusUtil;
    private final ServiceBusPayloadConverter payloadConverter;
    private final NextMoveQueue nextMoveQueue;

    @PostConstruct
    public void init() {
        String connectionString = String.format("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s",
                properties.getNextmove().getServiceBus().getBaseUrl(),
                properties.getNextmove().getServiceBus().getSasKeyName(),
                serviceBusUtil.getSasKey());

        ServiceBusProcessorClient serviceBusProcessorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .retryOptions(new AmqpRetryOptions()
                        .setDelay(Duration.ofSeconds(3))
                        .setMaxRetries(Integer.MAX_VALUE))
                .processor()
                .queueName(serviceBusUtil.getLocalQueuePath())
                .maxConcurrentCalls(50)
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .disableAutoComplete()
                .processMessage(this::processMessage)
                .processError(this::processError)
                .buildProcessorClient();
        serviceBusProcessorClient.start();
    }

    private void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage m = context.getMessage();
        log.debug(format("Received message on queue=%s with id=%s", serviceBusUtil.getLocalQueuePath(), m.getMessageId()));
        ServiceBusPayload payload;
        try {
            payload = payloadConverter.convert(m.getBody().toBytes());
        } catch (IOException e) {
            log.error(String.format("Failed to convert servicebus message with id = %s, abandoning", m.getMessageId()), e);
            context.deadLetter();
            return;
        }
        ByteArrayResource asicResource = (payload.getAsic() != null)
                ? new ByteArrayResource(Base64.getDecoder().decode(payload.getAsic()))
                : null;
        nextMoveQueue.enqueueIncomingMessage(payload.getSbd(), DPE, asicResource);
        context.complete();
    }

    private void processError(ServiceBusErrorContext context) {
        log.error("Error when receiving messages from namespace: '{}'. Entity: '{}'",
                context.getFullyQualifiedNamespace(), context.getEntityPath());

        if (!(context.getException() instanceof ServiceBusException)) {
            log.error("Non-ServiceBusException occurred", context.getException());
            return;
        }

        ServiceBusException exception = (ServiceBusException) context.getException();
        ServiceBusFailureReason reason = exception.getReason();

        if (reason == ServiceBusFailureReason.MESSAGING_ENTITY_DISABLED
                || reason == ServiceBusFailureReason.MESSAGING_ENTITY_NOT_FOUND
                || reason == ServiceBusFailureReason.UNAUTHORIZED) {
            log.error("An unrecoverable error occurred. Stopping processing with reason {}: {}", reason, exception.getMessage());
        } else if (reason == ServiceBusFailureReason.MESSAGE_LOCK_LOST) {
            log.error("Message lock lost for message", context.getException());
        } else if (reason == ServiceBusFailureReason.SERVICE_BUSY) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error("Unable to sleep");
            }
        } else {
            log.error(String.format("Error source %s, reason %s", context.getErrorSource(), reason), context.getException());
        }
    }

}
