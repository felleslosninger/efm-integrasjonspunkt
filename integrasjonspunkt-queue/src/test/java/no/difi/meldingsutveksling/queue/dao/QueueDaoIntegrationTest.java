package no.difi.meldingsutveksling.queue.dao;

import no.difi.meldingsutveksling.queue.config.QueueConfig;
import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = QueueConfig.class)
public class QueueDaoIntegrationTest {
    @Autowired
    private QueueDao queueDao;

    @Test
    public void shouldWriteMetadataToDatabaseWhenNewEntry() {
        queueDao.saveEntry(createQueue());
        Queue retrieve = queueDao.retrieve(Status.FAILED);

        System.out.println(retrieve.getUnique());
        System.out.println(retrieve.getChecksum());

        fail();
    }

    private static Queue createQueue() {
        return new Queue.Builder()
                .unique("unique1")
                .numberAttempt(2)
                .rule(RuleDefault.getRule())
                .checksum("checksum")
                .lastAttemptTime(new Date())
                .location("file.123.xml")
                .status(Status.NEW)
                .build();
    }
}