package no.difi.meldingsutveksling.eventlog;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class PersistentEventLog extends EventLog {

    @Autowired
    private EventLogDAO eventLogDAO;

    @Override
    public void log(Event event) {
        eventLogDAO.insertEventLog(event);
    }

    EventLogDAO getEventLogDAO() {
        return eventLogDAO;
    }

    void setEventLogDAO(EventLogDAO eventLogDAO) {
        this.eventLogDAO = eventLogDAO;
    }
}
