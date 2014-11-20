package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.eventlog.*;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

@Component
@Profile("heroku")
public class LogToEventLogOnlySendMessageTemplate implements ISendMessageTemplate {

    @Autowired
    EventLog eventLog;

    @Override
    public PutMessageResponseType sendMessage(PutMessageRequestType message) {

        Marshaller m;
        String textMessage;

        try {
            JAXBContext context = JAXBContext.newInstance(PutMessageRequestType.class);
            m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            m.marshal(message, os);
            textMessage = new String(os.toByteArray(), "UTF-8");

        } catch (JAXBException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            return null;
        }

        Event e = new Event().setTimeStamp(System.currentTimeMillis()).setUuid(UUID.randomUUID()).setMessage(textMessage).setProcessStates(ProcessState.LEVERINGS_KVITTERING_SENT);
        EventLogDAO dao = new EventLogDAO(new HerokuDatabaseConfig().getDataSource());
        dao.insertEventLog(e);
        return new PutMessageResponseType();
    }
}
