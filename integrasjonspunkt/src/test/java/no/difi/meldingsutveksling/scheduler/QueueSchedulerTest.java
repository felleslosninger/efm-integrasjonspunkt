package no.difi.meldingsutveksling.scheduler;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.queue.domain.QueueElement;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;
import no.difi.meldingsutveksling.queue.service.Queue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class QueueSchedulerTest {
    public static final String UNIQUE_ID = "unique1";

    @InjectMocks
    private QueueScheduler queueScheduler;

    @Mock private Queue queueMock;
    @Mock private IntegrasjonspunktImpl integrasjonspunktMock;
    @Mock private IntegrasjonspunktConfiguration integrasjonspunktConfigMock;

    @Before
    public void setUp() {
        initMocks(this);

        when(integrasjonspunktConfigMock.isQueueEnabled()).thenReturn(true);

        queueScheduler = new QueueScheduler(queueMock, integrasjonspunktMock, integrasjonspunktConfigMock);
    }

    @Test
    public void shouldGetNextFromQueueWhenSendMessageSchedulerTriggers() {
        queueScheduler.sendMessage();

        verify(queueMock, times(1)).getNext();
    }

    @Test
    public void shouldAttemptToSendMessageWhenNextItemFoundOnQueue() throws Exception {
        QueueElement element = createQueue(UNIQUE_ID, Status.NEW);
        PutMessageRequestType requestType = new PutMessageRequestType();

        when(queueMock.getNext()).thenReturn(element).thenReturn(null);
        when(queueMock.getMessage(element.getUniqueId())).thenReturn(requestType);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(true);

        queueScheduler.sendMessage();

        verify(integrasjonspunktMock, times(1)).sendMessage(requestType);
    }

    @Test
    public void shouldUpdateResultToQueueWithStatusForSuccessWhenMessageIsSent() {
        QueueElement element = createQueue(UNIQUE_ID, Status.NEW);

        when(queueMock.getNext()).thenReturn(element).thenReturn(null);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(true);

        queueScheduler.sendMessage();

        verify(queueMock, times(1)).success(element.getUniqueId());
        verify(queueMock, never()).fail(anyString());
    }

    @Test
    public void shouldUpdateResultToQueueWithStatusFailedWhenMessageFails() {
        QueueElement element = createQueue(UNIQUE_ID, Status.NEW);

        when(queueMock.getNext()).thenReturn(element).thenReturn(null);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(false);

        queueScheduler.sendMessage();

        verify(queueMock, never()).success(anyString());
        verify(queueMock, times(1)).fail(element.getUniqueId());
    }

    @Test
    public void shouldGetNextFromQueueWhenRetryQueueIsTriggered() {
        queueScheduler.sendMessage();

        verify(queueMock, times(1)).getNext();
    }

    @Test
    public void shouldAttemptToResendMessageWhenNextRetryMessageIsFound() throws Exception {
        QueueElement element = createQueue(UNIQUE_ID, Status.RETRY);
        PutMessageRequestType requestType = new PutMessageRequestType();

        when(queueMock.getNext()).thenReturn(element).thenReturn(null);
        when(queueMock.getMessage(element.getUniqueId())).thenReturn(requestType);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(true);

        queueScheduler.sendMessage();

        verify(integrasjonspunktMock, times(1)).sendMessage(requestType);
        verify(queueMock, times(2)).getNext();
    }

    @Test
    public void shouldProcessNewAndRetryWhenBothAreInQueue() throws Exception {
        QueueElement retryElement = createQueue("retry", Status.RETRY);
        QueueElement newElement = createQueue("new", Status.NEW);
        PutMessageRequestType requestType = new PutMessageRequestType();

        when(queueMock.getNext()).thenReturn(retryElement).thenReturn(newElement).thenReturn(null);
        when(queueMock.getMessage(anyString())).thenReturn(requestType);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(true);

        queueScheduler.sendMessage();

        verify(integrasjonspunktMock, times(2)).sendMessage(requestType);
        verify(queueMock, times(3)).getNext();
    }

    @Test
    public void shouldUpdateResultToQueueWithStatusForSuccessWhenRetryMessageIsSent() {
        QueueElement element = createQueue(UNIQUE_ID, Status.RETRY);

        when(queueMock.getNext()).thenReturn(element).thenReturn(null);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(true);

        queueScheduler.sendMessage();

        verify(queueMock, times(1)).success(element.getUniqueId());
        verify(queueMock, never()).fail(anyString());
    }

    @Test
    public void shouldUpdateResultToQueueWithStatusFailedWhenRetryMessageFails() {
        QueueElement element = createQueue(UNIQUE_ID, Status.RETRY);

        when(queueMock.getNext()).thenReturn(element).thenReturn(null);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(false);

        queueScheduler.sendMessage();

        verify(queueMock, never()).success(anyString());
        verify(queueMock, times(1)).fail(element.getUniqueId());
    }

    private static QueueElement createQueue(String uniqueId, Status status) {
        return new QueueElement.Builder()
                .uniqueId(uniqueId)
                .numberAttempt(0)
                .rule(RuleDefault.getRule())
                .status(status)
                .location("file_on.disk")
                .lastAttemptTime(new Date())
                .checksum("12345")
                .build();
    }
}