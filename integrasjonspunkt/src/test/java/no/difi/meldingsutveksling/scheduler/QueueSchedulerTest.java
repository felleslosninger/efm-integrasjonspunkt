package no.difi.meldingsutveksling.scheduler;

import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;
import no.difi.meldingsutveksling.queue.service.QueueService;
import org.junit.Before;
import org.junit.Test;
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
    private QueueScheduler queueScheduler;

    @Mock private QueueService queueServiceMock;
    @Mock private IntegrasjonspunktImpl integrasjonspunktMock;

    @Before
    public void setUp() {
        initMocks(this);

        queueScheduler = new QueueScheduler(queueServiceMock, integrasjonspunktMock);
    }

    @Test
    public void shouldGetNextNewFromQueueWhenSendMessageSchedulerTriggers() {
        when(queueServiceMock.getNext(Status.NEW)).thenReturn(createQueue(UNIQUE_ID, Status.NEW));

        queueScheduler.sendMessage();

        verify(queueServiceMock, times(1)).getNext(Status.NEW);
    }

    @Test
    public void shouldAttemptToSendMessageWhenNextItemFoundOnQueue() {
        Queue element = createQueue(UNIQUE_ID, Status.NEW);
        PutMessageRequestType requestType = new PutMessageRequestType();

        when(queueServiceMock.getNext(Status.NEW)).thenReturn(element);
        when(queueServiceMock.getMessage(element.getUnique())).thenReturn(requestType);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(true);

        queueScheduler.sendMessage();

        verify(integrasjonspunktMock, times(1)).sendMessage(requestType);
    }

    @Test
    public void shouldUpdateResultToQueueWithStatusForSuccessWhenMessageIsSent() {
        Queue element = createQueue(UNIQUE_ID, Status.NEW);

        when(queueServiceMock.getNext(Status.NEW)).thenReturn(element);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(true);

        queueScheduler.sendMessage();

        verify(queueServiceMock, times(1)).success(element.getUnique());
        verify(queueServiceMock, never()).fail(anyString());
    }

    @Test
    public void shouldUpdateResultToQueueWithStatusFailedWhenMessageFails() {
        Queue element = createQueue(UNIQUE_ID, Status.NEW);

        when(queueServiceMock.getNext(Status.NEW)).thenReturn(element);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(false);

        queueScheduler.sendMessage();

        verify(queueServiceMock, never()).success(anyString());
        verify(queueServiceMock, times(1)).fail(element.getUnique());
    }

    @Test
    public void shouldGetNextFromQueueWhenRetryQueueIsTriggered() {
        when(queueServiceMock.getNext(Status.FAILED)).thenReturn(createQueue(UNIQUE_ID, Status.FAILED));

        queueScheduler.retryMessages();

        verify(queueServiceMock, times(1)).getNext(Status.FAILED);
    }

    @Test
    public void shouldAttemptToResendMessageWhenNextRetryMessageIsFound() {
        Queue element = createQueue(UNIQUE_ID, Status.FAILED);
        PutMessageRequestType requestType = new PutMessageRequestType();

        when(queueServiceMock.getNext(Status.FAILED)).thenReturn(element);
        when(queueServiceMock.getMessage(element.getUnique())).thenReturn(requestType);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(true);

        queueScheduler.retryMessages();

        verify(integrasjonspunktMock, times(1)).sendMessage(requestType);
    }

    @Test
    public void shouldUpdateResultToQueueWithStatusForSuccessWhenRetryMessageIsSent() {
        Queue element = createQueue(UNIQUE_ID, Status.FAILED);

        when(queueServiceMock.getNext(Status.FAILED)).thenReturn(element);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(true);

        queueScheduler.retryMessages();

        verify(queueServiceMock, times(1)).success(element.getUnique());
        verify(queueServiceMock, never()).fail(anyString());
    }

    @Test
    public void shouldUpdateResultToQueueWithStatusFailedWhenRetryMessageFails() {
        Queue element = createQueue(UNIQUE_ID, Status.FAILED);

        when(queueServiceMock.getNext(Status.FAILED)).thenReturn(element);
        when(integrasjonspunktMock.sendMessage(any(PutMessageRequestType.class))).thenReturn(false);

        queueScheduler.retryMessages();

        verify(queueServiceMock, never()).success(anyString());
        verify(queueServiceMock, times(1)).fail(element.getUnique());
    }

    private static Queue createQueue(String uniqueId, Status status) {
        return new Queue.Builder()
                .unique(uniqueId)
                .numberAttempt(0)
                .rule(RuleDefault.getRule())
                .status(status)
                .location("file_on.disk")
                .lastAttemptTime(new Date())
                .checksum("12345")
                .build();
    }
}