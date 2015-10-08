package no.difi.meldingsutveksling.eventlog;

import no.difi.meldingsutveksling.domain.ProcessState;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * These tests use flyway to establish an in memory database before they are run.
 */
@Ignore("Since IntegrasjonspunktReceiveImpl now is a Spring bean and it instantiates an instance of " +
        "KeyConfiguration we would need to provide environment variables: privatekeyalias, privatekeyloacation " +
        "and privatekeypassword. But we do not want to need that to run this junit test. This test needs to be " +
        "rewritten or removed")
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
        e.setMessage("this is a message)");
        eventLogDAO.insertEventLog(e);
    }
}
