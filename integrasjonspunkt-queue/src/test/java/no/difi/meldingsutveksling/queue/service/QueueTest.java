package no.difi.meldingsutveksling.queue.service;

import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.domain.QueueElement;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Date;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.queue.messageutil.QueueMessageFile.FILE_PATH;
import static no.difi.meldingsutveksling.queue.objectmother.QueueObjectMother.createQueue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class QueueTest {
    private static final String NOT_ENCRYPTED_TEST_STRING = "TestObject";
    public static final String UNIQUE_ID = "1";

    private Queue queue;

    @Mock private QueueDao queueDaoMock;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        queue = new Queue(queueDaoMock);
    }

    @Test
    public void shouldSaveMetadataWhenSavingEntryOnQueue() throws IOException {
        queue.put(NOT_ENCRYPTED_TEST_STRING);

        verify(queueDaoMock, times(1)).saveEntry(any(QueueElement.class));
    }

    @Test
    public void shouldLoadMetadataForNewWhenRetrievingFromQueue() {
        when(queueDaoMock.retrieve(Status.NEW)).thenReturn(asList(createQueue("1", Status.NEW)));

        QueueElement actual = queue.getNext();

        verify(queueDaoMock, times(1)).retrieve(Status.NEW);
        assertEquals(actual.getStatus(), Status.NEW);
    }

    @Test
    public void shouldLoadMetadataForRetryWhenRetrievingFromQueue() {
        when(queueDaoMock.retrieve(Status.RETRY)).thenReturn(asList(createQueue("1", Status.RETRY)));

        QueueElement actual = queue.getNext();

        verify(queueDaoMock, times(1)).retrieve(Status.RETRY);
        assertEquals(actual.getStatus(), Status.RETRY);
    }

    @Test
    public void shouldGetMessageWhenRequested() throws IOException {
        String filename = FILE_PATH + createEncryptedFile();
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, filename));

        Object message = queue.getMessage(UNIQUE_ID);

        assertEquals(NOT_ENCRYPTED_TEST_STRING, message);
    }

    @Test
    public void shouldRemoveFileWhenSuccessReported() throws IOException {
        String filename = FILE_PATH + createEncryptedFile();
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, filename));

        queue.success(UNIQUE_ID);

        assertFalse(new File(filename).exists());
    }

    @Test
    public void shouldUpdateStatusOnQueueWhenSuccessReported() throws Exception {
        when(queueDaoMock.retrieve(anyString())).thenReturn(
                createQueue(UNIQUE_ID, Status.NEW, QueueDao.addMinutesToDate(new Date(), -60)));

        queue.success(UNIQUE_ID);

        verify(queueDaoMock, times(1)).updateStatus(any(QueueElement.class));
    }

    @Test
    public void shouldKeepFileWhenFailReportedAndMaxAttemptNotExceeded() throws IOException {
        String filename = FILE_PATH + createEncryptedFile();
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, filename));

        queue.fail(UNIQUE_ID);

        assertTrue(new File(filename).exists());
    }

    @Test
    public void shouldUpdateCounterForNumberOfErrorsWhenFailReported() {
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, 1));
        ArgumentCaptor<QueueElement> args = ArgumentCaptor.forClass(QueueElement.class);

        queue.fail(UNIQUE_ID);

        verify(queueDaoMock, times(1)).retrieve(anyString());
        verify(queueDaoMock, times(1)).updateEntry(args.capture());

        QueueElement actual = args.getValue();
        assertEquals(2, actual.getNumberAttempts());
    }

    @Test
    public void shouldNotFailWithLastAttemptWhenRuleNotExceededNumberOfRetries() {
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, 1));
        ArgumentCaptor<QueueElement> args = ArgumentCaptor.forClass(QueueElement.class);

        queue.fail(UNIQUE_ID);

        verify(queueDaoMock, times(1)).updateEntry(args.capture());

        QueueElement actual = args.getValue();
        assertTrue(actual.getRule().getMaxAttempt() > actual.getNumberAttempts());
        assertEquals(actual.getStatus(), Status.RETRY);
    }

    @Test
    public void shouldNotFailWithLastAttemptWhenRuleIsEqualToNumberOfRetriesAndKeepStatusFailed() {
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, 2));
        ArgumentCaptor<QueueElement> args = ArgumentCaptor.forClass(QueueElement.class);

        queue.fail(UNIQUE_ID);

        verify(queueDaoMock, times(1)).updateEntry(args.capture());

        QueueElement actual = args.getValue();
        assertEquals(actual.getNumberAttempts(), actual.getRule().getMaxAttempt());
        assertEquals(actual.getStatus(), Status.RETRY);
    }

    @Test
    public void shouldFailWithLastAttemptWhenRuleExceededNumberOfRetriesAndUpdateStatusToPermanentError() {
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, 4));
        ArgumentCaptor<QueueElement> args = ArgumentCaptor.forClass(QueueElement.class);

        queue.fail(UNIQUE_ID);

        verify(queueDaoMock, times(1)).updateEntry(args.capture());

        QueueElement actual = args.getValue();
        assertTrue(actual.getRule().getMaxAttempt() <= actual.getNumberAttempts());
        assertEquals(Status.ERROR, actual.getStatus());
    }

    @Test
    public void shouldSetLastAttemptedTimeWhenFailReported() throws Exception {
        Date oldDate = QueueDao.addMinutesToDate(new Date(), -60);
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, oldDate));
        ArgumentCaptor<QueueElement> args = ArgumentCaptor.forClass(QueueElement.class);

        queue.fail(UNIQUE_ID);

        verify(queueDaoMock, times(1)).updateEntry(args.capture());

        QueueElement actual = args.getValue();
        assertFalse(oldDate.getTime() == actual.getLastAttemptTime().getTime());
    }

    @Test
    public void shouldRemoveFileWhenFailReportedAndMaxAttemptsIsExceeded() throws IOException {
        String filename = FILE_PATH + createEncryptedFile();
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, RuleDefault.getRule().getMaxAttempt(), filename));

        queue.fail(UNIQUE_ID);

        assertFalse(new File(filename).exists());
    }

    private String createEncryptedFile() throws IOException {
        queue.put(NOT_ENCRYPTED_TEST_STRING);
        File dir = new File(FILE_PATH);

        File[] files = dir.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".queue");
            }
        });
        return files[0].getName();
    }

    @AfterClass
    public static void cleanUp() {
        String filePath = FileSystems.getDefault().getPath(FILE_PATH).toString();

        File file = new File(filePath);

        file.delete();
        if (file.exists()) {
            System.out.println(String.format("Cleanup of file %s failed. Manually cleanup necessary! ", filePath));
        }
    }
}