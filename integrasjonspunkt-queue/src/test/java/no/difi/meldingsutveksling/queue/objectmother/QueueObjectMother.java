package no.difi.meldingsutveksling.queue.objectmother;

import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class QueueObjectMother {
    public static Queue createQueue(String unique, Date lastAttemptTime) {
        return createQueueBuilder().unique(unique).lastAttemptTime(lastAttemptTime).build();
    }

    public static Queue createQueue(String unique, Status status) {
        return createQueueBuilder().unique(unique).status(status).build();
    }

    public static Queue createQueue(String unique) {
        return createQueueBuilder().unique(unique).build();
    }

    public static Queue createQueue(String unique, String filename) {
        return createQueueBuilder().unique(unique).location(filename).build();
    }

    public static void assertQueue(Queue expected, Queue result) {
        assertEquals(result.getRuleName(), expected.getRuleName());
        assertEquals(result.getChecksum(), expected.getChecksum());
        assertEquals(result.getUnique(), expected.getUnique());
        assertEquals(result.getLastAttemptTime().getTime(), expected.getLastAttemptTime().getTime());
        assertEquals(result.getNumberAttempts(), expected.getNumberAttempts());
        assertEquals(result.getStatus(), expected.getStatus());
        assertEquals(result.getRequestLocation(), expected.getRequestLocation());
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
}