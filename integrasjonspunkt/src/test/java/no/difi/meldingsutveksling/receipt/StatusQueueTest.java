package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusQueueTest {

    @Mock
    private JmsTemplate jmsTemplate;

    private StatusQueue statusQueue;
    private MessageStatus status;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        IntegrasjonspunktProperties props = new IntegrasjonspunktProperties();
        props.setFeature(new IntegrasjonspunktProperties.FeatureToggle());
        props.getFeature().setStatusQueueIncludes(Set.of(ServiceIdentifier.DPO));
        statusQueue = new StatusQueue(jmsTemplate, props);

        status = mock(MessageStatus.class);
        when(status.getStatus()).thenReturn(ReceiptStatus.LEVERT.name());
        when(status.getId()).thenReturn(42L);
        conversation = mock(Conversation.class);
        when(conversation.getServiceIdentifier()).thenReturn(ServiceIdentifier.DPO);
        when(conversation.getMessageStatuses()).thenReturn(Set.of(status));
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sendsImmediatelyWithoutTransaction() {
        statusQueue.enqueueStatus(status, conversation);

        verify(jmsTemplate).send(eq("STATUS"), any(MessageCreator.class));
    }

    @Test
    void sendsOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        statusQueue.enqueueStatus(status, conversation);
        verify(jmsTemplate, never()).send(eq("STATUS"), any(MessageCreator.class));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(jmsTemplate).send(eq("STATUS"), any(MessageCreator.class));
    }

    @Test
    void doesNotSendOnRollback() {
        TransactionSynchronizationManager.initSynchronization();

        statusQueue.enqueueStatus(status, conversation);
        TransactionSynchronizationManager.getSynchronizations()
            .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(jmsTemplate, never()).send(eq("STATUS"), any(MessageCreator.class));
    }

    @Test
    void sendFailureAfterCommitIsNotPropagated() {
        doThrow(new UncategorizedJmsException("broker down")).when(jmsTemplate).send(eq("STATUS"), any(MessageCreator.class));
        TransactionSynchronizationManager.initSynchronization();

        statusQueue.enqueueStatus(status, conversation);

        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit));
    }
}
