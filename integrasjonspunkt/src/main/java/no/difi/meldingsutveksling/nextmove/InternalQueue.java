package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.QueueInterruptException;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.logging.MessageMarkerFactory;
import no.difi.meldingsutveksling.noarkexchange.IntegrajonspunktReceiveImpl;
import no.difi.meldingsutveksling.pipes.PromiseRuntimeException;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Session;
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

    private static final String NOARK = "noark";
    private static final String NEXTMOVE = "nextmove";
    private static final String DLQ = "ActiveMQ.DLQ";

    private final JmsTemplate jmsTemplate;
    private final NextMoveSender nextMoveSender;
    private final ObjectMapper objectMapper;
    private final IntegrajonspunktReceiveImpl integrajonspunktReceive;
    private final DocumentConverter documentConverter;
    private final DeadLetterQueueHandler deadLetterQueueHandler;
    private final MessageMarkerFactory messageMarkerFactory;

    public InternalQueue(JmsTemplate jmsTemplate,
                         NextMoveSender nextMoveSender,
                         ObjectMapper objectMapper,
                         ObjectProvider<IntegrajonspunktReceiveImpl> integrajonspunktReceive,
                         DocumentConverter documentConverter,
                         DeadLetterQueueHandler deadLetterQueueHandler, MessageMarkerFactory messageMarkerFactory) {
        this.jmsTemplate = jmsTemplate;
        this.nextMoveSender = nextMoveSender;
        this.objectMapper = objectMapper;
        this.integrajonspunktReceive = integrajonspunktReceive.getIfAvailable();
        this.documentConverter = documentConverter;
        this.deadLetterQueueHandler = deadLetterQueueHandler;
        this.messageMarkerFactory = messageMarkerFactory;
    }

    @JmsListener(destination = NEXTMOVE, containerFactory = "myJmsContainerFactory")
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

    @JmsListener(destination = NOARK, containerFactory = "myJmsContainerFactory")
    public void noarkListener(byte[] message, Session session) {
        StandardBusinessDocument sbd = documentConverter.unmarshallFrom(message);
        MDC.put(NextMoveConsts.CORRELATION_ID, SBDUtil.getMessageId(sbd));
        try {
            integrajonspunktReceive.forwardToNoarkSystem(sbd);
        } catch (Exception e) {
            Audit.warn("Failed delivering message to archive.. queue will retry",
                    messageMarkerFactory.markerFrom(sbd), e);
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    /**
     * Log failed messages as errors
     *
     * @param message the message
     * @param session {@link Session}
     */
    @JmsListener(destination = DLQ)
    public void dlqListener(byte[] message, Session session) {
        deadLetterQueueHandler.handleDlqMessage(message);
    }

    public void enqueueNextMove(NextMoveOutMessage msg) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            objectMapper.writeValue(bos, msg);
            jmsTemplate.convertAndSend(NEXTMOVE, bos.toByteArray());
        } catch (IOException e) {
            throw new NextMoveRuntimeException(String.format("Unable to marshall NextMove message with id=%s", msg.getMessageId()), e);
        }
    }

    /**
     * Places the input parameter on the NOARK queue. The NOARK queue sends messages from external sender to NOARK server.
     *
     * @param sbd the sbd as received by IntegrasjonspunktReceiveImpl from an external source
     */
    public void enqueueNoark(StandardBusinessDocument sbd) {
        jmsTemplate.convertAndSend(NOARK, documentConverter.marshallToBytes(sbd));
    }
}
