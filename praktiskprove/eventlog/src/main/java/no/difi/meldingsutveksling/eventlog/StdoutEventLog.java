package no.difi.meldingsutveksling.eventlog;

class StdoutEventLog extends EventLog {
    @Override
    public void log(Event event) {
        System.out.println(event.toString());
    }
}
