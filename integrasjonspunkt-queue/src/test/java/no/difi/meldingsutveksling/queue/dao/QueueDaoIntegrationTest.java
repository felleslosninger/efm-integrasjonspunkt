package no.difi.meldingsutveksling.queue.dao;

import no.difi.meldingsutveksling.queue.config.QueueConfig;
import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = QueueConfig.class)
public class QueueDaoIntegrationTest {
    @Autowired
    private QueueDao queueDao;

    @Test
    public void shouldWriteAndReadMetadataToDatabaseWhenNewEntry() {
        Queue expected = createQueue("unique1");

        queueDao.saveEntry(expected);
        Queue actual = queueDao.retrieve(Status.NEW).get(0);

        assertQueue(expected, actual);
    }

    @Test
    public void shouldWriteTwoEntriesWithStatusNewToDatabaseAndRetrieveBothWhenStatusIsNew() {
        Queue expected0 = createQueue("uniqueA1");
        Queue expected1 = createQueue("uniqueA2");

        queueDao.saveEntry(expected0);
        queueDao.saveEntry(expected1);
        List<Queue> retrieve = queueDao.retrieve(Status.NEW);

        assertQueue(expected0, retrieve.get(0));
        assertQueue(expected1, retrieve.get(1));
    }

    @Test
    public void shouldOnlyRetrieveWithStatusFailedWhenRequestedStatusFailed() {
        Queue expected0 = createQueue("uniqueB1", Status.NEW);
        Queue expected1 = createQueue("uniqueB2", Status.FAILED);

        queueDao.saveEntry(expected0);
        queueDao.saveEntry(expected1);
        List<Queue> retrieve = queueDao.retrieve(Status.FAILED);

        assertEquals(1, retrieve.size());
        assertQueue(expected1, retrieve.get(0));
    }

    @Test
    public void shouldUpdateStatus() {
        queueDao.saveEntry(createQueue("uniqueC1", Status.NEW));
        queueDao.updateStatus("uniqueC1", Status.FAILED);
        Queue actual = queueDao.retrieve(Status.FAILED).get(0);

        assertEquals(Status.FAILED, actual.getStatus());
    }

    @Test
    public void shouldRetrieveResultBasedOnStatusSortedOnTimestamp() throws Exception {
        Date now = date(0);
        Date tomorrow = date(1);
        Date yesterday = date(-1);
        queueDao.saveEntry(createQueue("uniqueD1", now));
        queueDao.saveEntry(createQueue("uniqueD2", tomorrow));
        queueDao.saveEntry(createQueue("uniqueD3", yesterday));

        List<Queue> actual = queueDao.retrieve(Status.NEW);

        assertEquals(yesterday.getTime(), actual.get(0).getLastAttemptTime().getTime());
        assertEquals(now.getTime(), actual.get(1).getLastAttemptTime().getTime());
        assertEquals(tomorrow.getTime(), actual.get(2).getLastAttemptTime().getTime());
    }

    @After
    public void tearDown() {
        queueDao.removeAll();
    }

    private Date date(int addDays) throws Exception {
        String dt = "2015-01-01";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTime(sdf.parse(dt));
        c.add(Calendar.DATE, addDays);
        return c.getTime();
    }

    private Queue createQueue(String unique, Date lastAttemptTime) {
        return createQueueBuilder().unique(unique).lastAttemptTime(lastAttemptTime).build();
    }

    private Queue createQueue(String unique, Status status) {
        return createQueueBuilder().unique(unique).status(status).build();
    }

    private Queue createQueue(String unique) {
        return createQueueBuilder().unique(unique).build();
    }

    private static Queue.Builder createQueueBuilder() {
        return new Queue.Builder()
                .unique("unique1")
                .numberAttempt(2)
                .rule(RuleDefault.getRule())
                .checksum("checksum")
                .lastAttemptTime(new Date())
                .location("file.123.xml")
                .status(Status.NEW);
    }

    private static void assertQueue(Queue expected, Queue result) {
        assertEquals(result.getRuleName(), expected.getRuleName());
        assertEquals(result.getChecksum(), expected.getChecksum());
        assertEquals(result.getUnique(), expected.getUnique());
        assertEquals(result.getLastAttemptTime().getTime(), expected.getLastAttemptTime().getTime());
        assertEquals(result.getNumberAttempts(), expected.getNumberAttempts());
        assertEquals(result.getStatus(), expected.getStatus());
        assertEquals(result.getRequestLocation(), expected.getRequestLocation());
    }
}