package no.difi.meldingsutveksling.eventlog;

import java.util.Date;
import java.util.UUID;

public class Event {
    private UUID uuid;
    private  int state;
    private java.sql.Date timeStamp;

    public UUID getUuid() {
        return uuid;
    }

    public Event setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public int getState() {
        return state;
    }

    public Event setState(int state) {
        this.state = state;
        return this;
    }

    public java.sql.Date getTimeStamp() {
        return timeStamp;
    }

    public Event setTimeStamp(java.sql.Date timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }
}
