package no.difi.meldingsutveksling.receipt;

import jakarta.jms.TextMessage;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

@Component
public class StatusQueue {

    private final JmsTemplate jmsTemplate;
    private final IntegrasjonspunktProperties props;

    private final Logger log = LoggerFactory.getLogger(StatusQueue.class);
    private final String queueName = "STATUS";

    public StatusQueue(JmsTemplate jmsTemplate, IntegrasjonspunktProperties props) {
        this.jmsTemplate = jmsTemplate;
        this.props = props;
        this.jmsTemplate.setReceiveTimeout(500L);
        this.jmsTemplate.setSessionAcknowledgeMode(2);
    }

    public void enqueueStatus(MessageStatus status, Conversation conversation) {
        if (props.getFeature().getStatusQueueIncludes().contains(conversation.getServiceIdentifier())) {
            conversation.getMessageStatuses().stream()
                    .filter(ms -> ms.getStatus().equals(status.getStatus()))
                    .findFirst()
                    .map(MessageStatus::getId)
                    .ifPresent(this::sendAfterCommit);
        }
    }

    // The JMS send is not part of the database transaction. Sending before commit would leave the id on the queue
    // if the transaction is rolled back (e.g. deadlock retry), pointing to a status that never made it to the DB.
    private void sendAfterCommit(Long id) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(id);
                }
            });
        } else {
            send(id);
        }
    }

    private void send(Long id) {
        try {
            jmsTemplate.send(queueName, c -> {
                TextMessage m = c.createTextMessage(id.toString());
                m.setJMSCorrelationID(id.toString());
                return m;
            });
        } catch (JmsException e) {
            log.error("Failed to put MessageStatus with id={} on status queue, it is still available through /api/statuses", id, e);
        }
    }

    public Optional<Long> receiveStatus() {
        TextMessage tm = jmsTemplate.browse(queueName, (s, b) -> (TextMessage) b.getEnumeration().nextElement());
        if (tm != null) {
            log.debug("Message on status queue: {}", tm);
            try {
                return Optional.of(Long.parseLong(tm.getText()));
            } catch (jakarta.jms.JMSException e) {
                log.warn("Failed to parse TextMessage from status queue", e);
            }
        }
        return Optional.empty();
    }

    public boolean removeStatus(Long id) {
        String selector = "JMSCorrelationID='" + id + "'";
        TextMessage tm = jmsTemplate.browseSelected(queueName, selector, (s, b) -> (TextMessage) b.getEnumeration().nextElement());
        if (tm != null) {
            jmsTemplate.receiveSelected(queueName, selector);
            return true;
        }
        return false;
    }

}
