package no.difi.meldingsutveksling.eventlog;

/**
 * This class represents an Event Log. Subclasses log to different locations. The default implementation logs to stdout.
 *
 * @author Glenn Bech
 */
public abstract class EventLog {

    public abstract void log(Event event);

    public static EventLog create() {
        return new StdoutEventLog();
    }

}
