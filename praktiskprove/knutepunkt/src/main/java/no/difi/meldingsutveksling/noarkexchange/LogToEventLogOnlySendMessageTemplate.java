package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.eventlog.*;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("heroku")
public class LogToEventLogOnlySendMessageTemplate implements ISendMessageTemplate {

    @Autowired
    EventLog eventLog;

    @Override
    public PutMessageResponseType sendMessage(PutMessageRequestType message) {
        Event e = new Event().setTimeStamp(System.currentTimeMillis()).setUuid(UUID.randomUUID()).setMessage(message.toString()).setProcessStates(ProcessState.LEVERINGS_KVITTERING_SENT);
        EventLogDAO dao = new EventLogDAO(new HerokuDatabaseConfig().getDataSource());
        dao.insertEventLog(e);
        return new PutMessageResponseType();
    }
}
