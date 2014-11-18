package no.difi.meldingsutveksling.eventlog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

/**
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/spring-rest.xml"})

public class EventLogIntegrationTest {

    @Autowired
    private EventLogDAO dao;

    @Test
    public void shouldInsert() {
        Event e = new Event().setUuid(UUID.randomUUID()).setProcessStates(ProcessState.AAPNINGS_KVITTERING_SENT).setSender("111111111").setReceiver("222222222");
        dao.insertEventLog(e);
    }
}
