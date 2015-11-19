package no.difi.meldingsutveksling.queue.service;

import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.queue.objectmother.QueueObjectMother.createQueue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
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
    public void shouldSaveMetadataWhenSavingEntryOnQueue() {
        queueService.put(NOT_ENCRYPTED_TEST_STRING);

        verify(queueDaoMock, times(1)).saveEntry(any(Queue.class));
    }

    @Test
    public void shouldLoadMetadataWhenRetrievingFromQueue() {
        when(queueDaoMock.retrieve(Status.NEW)).thenReturn(
                asList(createQueue("1", new Date(DATE_20TH_OCT_2015)), createQueue("2", new Date(DATE_25TH_OCT_2015))));

        Queue next = queueService.getNext(Status.NEW);

        verify(queueDaoMock, times(1)).retrieve(Status.NEW);
        assertEquals(next.getLastAttemptTime().getTime(), DATE_20TH_OCT_2015);
    }

    @Ignore
    @Test
    public void shouldDecryptFileWhenLoadingEntryFromFile() {
        //This method will test both encryption and decryption
        String file = createEncryptedFile();
        when(queueDaoMock.retrieve(UNIQUE_ID)).thenReturn(createQueue(UNIQUE_ID, QueueService.FILE_PATH + file));

        queueService.getMessage(UNIQUE_ID);
        Object message = queueService.getMessage(UNIQUE_ID);

        assertEquals(message.toString(), NOT_ENCRYPTED_TEST_STRING);
    }

    private String createEncryptedFile() {
        queueService.put(NOT_ENCRYPTED_TEST_STRING); //Create encrypted file
        File dir = new File(QueueService.FILE_PATH);

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
        //TODO: Remove all files in queue folder
    }
}