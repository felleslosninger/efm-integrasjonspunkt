package no.difi.meldingsutveksling.eventlog;

import no.difi.meldingsutveksling.IntegrasjonspunktApplication;
import no.difi.meldingsutveksling.domain.ProcessState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * These tests use derby to establish an in memory database before they are run.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(IntegrasjonspunktApplication.class)
@WebIntegrationTest
@ActiveProfiles("test")
public class EventLogIntegrationTest {

    @Autowired
    private EventLogDAO eventLogDAO;

    @Test
    public void shouldInsert() {
        String jpTestID = "42";
        Event e = new Event().setProcessStates(ProcessState.AAPNINGS_KVITTERING_SENT).setSender("111111111").setReceiver("222222222");
        e.setHubConversationId("99999999999999");
        e.setArkiveConversationId("88888888888");
        e.setJpId(jpTestID);
        e.setMessage("this is a message)");
        eventLogDAO.insertEventLog(e);

        List<Event> entries = eventLogDAO.getEventEntries("\'"+jpTestID+"\'", ConversationIdTypes.JPID);
        assertEquals(1, entries.size());
    }
}
