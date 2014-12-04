package no.difi.meldingsutveksling.noarkexchange;

import com.thoughtworks.xstream.XStream;

import no.difi.meldingsutveksling.domain.SBD;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLogDAO;
import no.difi.meldingsutveksling.eventlog.HerokuDatabaseConfig;
import no.difi.meldingsutveksling.eventlog.ProcessState;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;

@Component
@Profile("heroku")
public class LogToEventLogOnlySendMessageTemplate extends SendMessageTemplate {

    @Override
    void sendSBD(StandardBusinessDocument sbd) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public PutMessageResponseType sendMessage(PutMessageRequestType message) {

        String textMessage;
        XStream xs = new XStream();
        textMessage = xs.toXML(message);
        Event e = new Event().setMessage(textMessage).setProcessStates(ProcessState.NO_ARKIVE_UNAVAILABLE);
        EventLogDAO dao = new EventLogDAO(new HerokuDatabaseConfig().getDataSource());
        dao.insertEventLog(e);
        return new PutMessageResponseType();
    }
}
