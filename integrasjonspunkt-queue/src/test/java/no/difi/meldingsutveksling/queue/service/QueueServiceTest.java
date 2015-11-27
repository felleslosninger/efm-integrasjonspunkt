package no.difi.meldingsutveksling.queue.service;

import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.messageutil.QueueMessageFile;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.queue.messageutil.QueueMessageFile.FILE_PATH;
import static no.difi.meldingsutveksling.queue.objectmother.QueueObjectMother.createQueue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class QueueServiceTest {
    private static final String NOT_ENCRYPTED_TEST_STRING = "TestObject";
    private static final long DATE_25TH_OCT_2015 = 61406118000000L;
    private static final long DATE_20TH_OCT_2015 = 61406550000000L;
    public static final String UNIQUE_ID = "1";

    private QueueService queueService;

    @Mock private QueueDao queueDaoMock;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        queueService = new QueueService(queueDaoMock);
    }

    @Test
    public void shouldSaveMetadataWhenSavingEntryOnQueue() throws IOException {
        queueService.put(NOT_ENCRYPTED_TEST_STRING);

        verify(queueDaoMock, times(1)).saveEntry(any(Queue.class));
    }

    @Test
    public void shouldLoadMetadataForNewWhenRetrievingFromQueue() {
        when(queueDaoMock.retrieve(Status.NEW)).thenReturn(asList(createQueue("1", Status.NEW)));

        Queue actual = queueService.getNext();

        verify(queueDaoMock, times(1)).retrieve(Status.NEW);
        verify(queueDaoMock, times(1)).retrieve(Status.RETRY);
        assertEquals(actual.getStatus(), Status.NEW);
    }

    @Test
    public void shouldLoadMetadataForRetryWhenRetrievingFromQueue() {
        when(queueDaoMock.retrieve(Status.RETRY)).thenReturn(asList(createQueue("1", Status.RETRY)));

        Queue actual = queueService.getNext();

        verify(queueDaoMock, never()).retrieve(Status.NEW);
        verify(queueDaoMock, times(1)).retrieve(Status.RETRY);
        assertEquals(actual.getStatus(), Status.RETRY);
    }

    @Ignore("Encrypt/decrypt is temporarily disabled")
    @Test
    public void shouldDecryptFileWhenLoadingEntryFromFile() throws IOException {
        //This method will test both encryption and decryption
        String file = createEncryptedFile();
        when(queueDaoMock.retrieve(UNIQUE_ID)).thenReturn(createQueue(UNIQUE_ID, FILE_PATH + file));

        queueService.getMessage(UNIQUE_ID);
        Object message = queueService.getMessage(UNIQUE_ID);

        assertEquals(message.toString(), NOT_ENCRYPTED_TEST_STRING);
    }

    @Test
    public void shouldGetMessageWhenRequested() throws IOException {
        String filename = FILE_PATH + createEncryptedFile();
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, filename));

        Object message = queueService.getMessage(UNIQUE_ID);

        assertEquals(NOT_ENCRYPTED_TEST_STRING, message);
    }

    @Test
    public void shouldRemoveFileWhenSuccessReported() throws IOException {
        String filename = FILE_PATH + createEncryptedFile();
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, filename));

        queueService.success(UNIQUE_ID);

        assertFalse(new File(filename).exists());
    }

    @Test
    public void shouldUpdateStatusObjectWhenSuccessReported() throws Exception {
        Queue queue = createQueue(UNIQUE_ID, Status.NEW, QueueDao.addMinutesToDate(new Date(), -60));
        when(queueDaoMock.retrieve(anyString())).thenReturn(queue);

        queueService.success(UNIQUE_ID);

        verify(queueDaoMock, times(1)).updateStatus(any(Queue.class));
        verify(queueDaoMock, never()).updateStatus(queue);
    }

    @Test
    public void shouldKeepFileWhenFailReportedAndMaxAttemptNotExceeded() throws IOException {
        String filename = FILE_PATH + createEncryptedFile();
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, filename));

        queueService.fail(UNIQUE_ID);

        assertTrue(new File(filename).exists());
    }

    @Test
    public void shouldUpdateCounterForNumberOfErrorsWhenFailReported() {
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, 1));
        ArgumentCaptor<Queue> args = ArgumentCaptor.forClass(Queue.class);

        queueService.fail(UNIQUE_ID);

        verify(queueDaoMock, times(1)).retrieve(anyString());
        verify(queueDaoMock, times(1)).updateEntry(args.capture());

        Queue actual = args.getValue();
        assertEquals(2, actual.getNumberAttempts());
    }

    @Test
    public void shouldNotFailWithLastAttemptWhenRuleNotExceededNumberOfRetries() {
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, 1));
        ArgumentCaptor<Queue> args = ArgumentCaptor.forClass(Queue.class);

        queueService.fail(UNIQUE_ID);

        verify(queueDaoMock, times(1)).updateEntry(args.capture());

        Queue actual = args.getValue();
        assertTrue(actual.getRule().getMaxAttempt() > actual.getNumberAttempts());
        assertEquals(actual.getStatus(), Status.RETRY);
    }

    @Test
    public void shouldNotFailWithLastAttemptWhenRuleIsEqualToNumberOfRetriesAndKeepStatusFailed() {
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, 2));
        ArgumentCaptor<Queue> args = ArgumentCaptor.forClass(Queue.class);

        queueService.fail(UNIQUE_ID);

        verify(queueDaoMock, times(1)).updateEntry(args.capture());

        Queue actual = args.getValue();
        assertEquals(actual.getNumberAttempts(), actual.getRule().getMaxAttempt()-1); //-1 because first attempt is 0 {0,1,2,3}
        assertEquals(actual.getStatus(), Status.RETRY);
    }

    @Test
    public void shouldFailWithLastAttemptWhenRuleExceededNumberOfRetriesAndUpdateStatusToPermanentError() {
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, 4));
        ArgumentCaptor<Queue> args = ArgumentCaptor.forClass(Queue.class);

        queueService.fail(UNIQUE_ID);

        verify(queueDaoMock, times(1)).updateEntry(args.capture());

        Queue actual = args.getValue();
        assertTrue(actual.getRule().getMaxAttempt() <= actual.getNumberAttempts());
        assertEquals(Status.ERROR, actual.getStatus());
    }

    @Test
    public void shouldSetLastAttemptedTimeWhenFailReported() throws Exception {
        Date yesterday = QueueDao.addMinutesToDate(new Date(), -60);
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, yesterday));
        ArgumentCaptor<Queue> args = ArgumentCaptor.forClass(Queue.class);

        queueService.fail(UNIQUE_ID);

        verify(queueDaoMock, times(1)).updateEntry(args.capture());

        Queue actual = args.getValue();
        assertFalse(yesterday.getTime() == actual.getLastAttemptTime().getTime());
    }

    @Test
    public void shouldRemoveFileWhenFailReportedAndMaxAttemptsIsExceeded() throws IOException {
        String filename = FILE_PATH + createEncryptedFile();
        when(queueDaoMock.retrieve(anyString())).thenReturn(createQueue(UNIQUE_ID, RuleDefault.getRule().getMaxAttempt(), filename));

        queueService.fail(UNIQUE_ID);

        assertFalse(new File(filename).exists());
    }

    private String createEncryptedFile() throws IOException {
        queueService.put(NOT_ENCRYPTED_TEST_STRING); //Create encrypted file
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
        String filePath = FILE_PATH;
        if (QueueMessageFile.IS_WINDOWS) {
            filePath = filePath.replace("/", "\\");
        }

        File downloadFile = new File(filePath);

        downloadFile.delete();
        if (downloadFile.exists()) {
            System.out.println(String.format("Cleanup of file %s failed. Manually cleanup necessary! ", filePath));
        }
    }
}