package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Context object for PutMessageRequests
 *
 * @author Glenn Bech
 */
public class PutMessageContext {

    private EventLog eventlog;

    private MessageSender messageSender;

    public PutMessageContext(EventLog eventlog, MessageSender messageSender) {
        this.eventlog = eventlog;
        this.messageSender = messageSender;
    }

    public EventLog getEventlog() {
        return eventlog;
    }

    public void setEventlog(EventLog eventlog) {
        this.eventlog = eventlog;
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }
}
