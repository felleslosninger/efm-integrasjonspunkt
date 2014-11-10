package no.difi.meldingsutveksling.eventlog;

public abstract class EventLog {

    public abstract void log(Event event);

    public static EventLog create() {
        return new StdoutEventLog();
    }

}
