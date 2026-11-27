package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Status MOTTATT/LEVERT must only reach the sender once the transaction that stored/removed the
 * message has committed (MOVE-5500).
 */
class ResponseStatusSenderTransactionTest {

    private final ResponseStatusSenderProxy proxy = mock(ResponseStatusSenderProxy.class);
    private final StandardBusinessDocument sbd = new StandardBusinessDocument();
    private ResponseStatusSender sender;

    @BeforeEach
    void setUp() {
        IntegrasjonspunktProperties props = new IntegrasjonspunktProperties();
        props.setNextmove(new IntegrasjonspunktProperties.NextMove());
        props.getNextmove().setStatusServices(EnumSet.of(ServiceIdentifier.DPO));
        sender = new ResponseStatusSender(proxy, props);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void withoutTransaction_statusIsSentImmediately() {
        sender.queue(sbd, ServiceIdentifier.DPO, ReceiptStatus.MOTTATT);

        verify(proxy, timeout(2000)).queue(sbd, ServiceIdentifier.DPO, ReceiptStatus.MOTTATT);
    }

    @Test
    void insideTransaction_statusIsSentAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        sender.queue(sbd, ServiceIdentifier.DPO, ReceiptStatus.LEVERT);

        verify(proxy, after(200).never()).queue(any(), any(), any());
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

        TransactionSynchronizationUtils.triggerAfterCommit();

        verify(proxy, timeout(2000)).queue(sbd, ServiceIdentifier.DPO, ReceiptStatus.LEVERT);
    }

    @Test
    void insideTransaction_statusIsNotSentOnRollback() {
        TransactionSynchronizationManager.initSynchronization();

        sender.queue(sbd, ServiceIdentifier.DPO, ReceiptStatus.LEVERT);

        TransactionSynchronizationUtils.invokeAfterCompletion(
            TransactionSynchronizationManager.getSynchronizations(), TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(proxy, after(300).never()).queue(any(), any(), any());
    }

    @Test
    void serviceWithoutStatus_registersNothing() {
        TransactionSynchronizationManager.initSynchronization();

        sender.queue(sbd, ServiceIdentifier.DPV, ReceiptStatus.MOTTATT);

        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
        verify(proxy, after(200).never()).queue(any(), any(), any());
    }
}
