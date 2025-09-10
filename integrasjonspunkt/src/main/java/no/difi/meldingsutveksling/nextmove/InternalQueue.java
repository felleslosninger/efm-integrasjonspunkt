package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.QueueInterruptException;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.move.common.io.pipe.PromiseRuntimeException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import jakarta.jms.Session;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * The idea behind this queue is to avoid loosing messages before they are saved in Noark System.
 * <p>
 * The way it works is that any exceptions that happens in after a message is put on the queue is re-sent to the JMS listener. If
 * the application is restarted the message is also resent.
 * <p>
 * The JMS listener has the responsibility is to forward the message to the archive system.
 */
@Component
@Slf4j
public class InternalQueue {


    @Value("${difi.move.queue.nextmove-name}")
    private String nextmoveQueue;

    private final JmsTemplate jmsTemplate;
    private final NextMoveSender nextMoveSender;
    private final ObjectMapper objectMapper;
    private final DocumentConverter documentConverter;
    private final DeadLetterQueueHandler deadLetterQueueHandler;

    public InternalQueue(JmsTemplate jmsTemplate,
                         NextMoveSender nextMoveSender,
                         ObjectMapper objectMapper,
                         DocumentConverter documentConverter,
                         DeadLetterQueueHandler deadLetterQueueHandler) {
        this.jmsTemplate = jmsTemplate;
        this.nextMoveSender = nextMoveSender;
        this.objectMapper = objectMapper;
        this.documentConverter = documentConverter;
        this.deadLetterQueueHandler = deadLetterQueueHandler;
    }

    @JmsListener(destination = "${difi.move.queue.nextmove-name}", containerFactory = "myJmsContainerFactory")
    public void nextMoveListener(byte[] message, Session session) {
        NextMoveOutMessage nextMoveMessage;
        try {
            nextMoveMessage = objectMapper.readValue(message, NextMoveOutMessage.class);
        } catch (IOException e) {
            throw new NextMoveRuntimeException("Unable to unmarshall NextMove message from queue", e);
        }
        try {
            MDC.put(NextMoveConsts.CORRELATION_ID, nextMoveMessage.getMessageId());
            nextMoveSender.send(nextMoveMessage);
        } catch (PromiseRuntimeException e) {
            if (e.getCause() instanceof QueueInterruptException) {
                log.error("Caught interrupting exception, registering error and removing message from queue. Error was: {}", e.getCause().getMessage());
                deadLetterQueueHandler.handleNextMoveMessage(nextMoveMessage, e.getCause().getMessage());
            } else {
                throw e;
            }
        } catch (NextMoveException e) {
            throw new NextMoveRuntimeException("Unable to send NextMove message", e);
        }
    }

    /**
     * Log failed messages as errors
     *
     * @param message the message
     * @param session {@link Session}
     */
    @JmsListener(destination = "${difi.move.queue.dlq-name}")
    public void dlqListener(byte[] message, Session session) {
        deadLetterQueueHandler.handleDlqMessage(message);
    }

    public void enqueueNextMove(NextMoveOutMessage msg) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            objectMapper.writeValue(bos, msg);
            jmsTemplate.convertAndSend(nextmoveQueue, bos.toByteArray());
        } catch (IOException e) {
            throw new NextMoveRuntimeException("Unable to marshall NextMove message with id=%s".formatted(msg.getMessageId()), e);
        }
    }

}
