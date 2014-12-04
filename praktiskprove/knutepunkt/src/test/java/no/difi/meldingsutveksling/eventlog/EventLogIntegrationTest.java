package no.difi.meldingsutveksling.eventlog;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * These tests use flyway to establish an in memory database before they are run.
 */


public class EventLogIntegrationTest {


    @Test
    public void shouldInsert() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles("dev");
        ctx.scan("no.difi");
        ctx.refresh();

        EventLogDAO eventLogDAO = ctx.getBean(EventLogDAO.class);
        Event e = new Event().setProcessStates(ProcessState.AAPNINGS_KVITTERING_SENT).setSender("111111111").setReceiver("222222222");
        e.setHubConversationId("99999999999999");
        e.setArkiveConversationId("88888888888");
        e.setJpId("7777777777777");
        eventLogDAO.insertEventLog(e);
    }
}
