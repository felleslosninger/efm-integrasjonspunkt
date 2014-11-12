package no.difi.meldingsutveksling.eventlog;


import java.sql.Timestamp;
import java.util.UUID;

public class Event {
    private UUID uuid;
    private long sender;
    private long receiver;
    private Timestamp timeStamp;
    private ProcessState processState;


    public UUID getUuid() {
        return uuid;
    }

    public Event setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public Timestamp getTimeStamp() {
        return timeStamp;
    }

    public Event setTimeStamp(Timestamp timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    public ProcessState getProcessState() {
        return processState;
    }

    public Event setProcessStates(ProcessState processState) {
        this.processState = processState;
        return this;
    }


    public long getSender() {
        return sender;
    }

    public Event setSender(long sender) {
        this.sender = sender;
        return this;
    }

    public long getReceiver() {
        return receiver;
    }

    public Event setReceiver(long receiver) {
        this.receiver = receiver;
        return this;
    }

    public String toString() {
        String toReturn= "UUID: " + uuid +" time: "+timeStamp+" sender: " +sender+" reciever: "+ receiver+ " process state: "+ processState;
        return toReturn;
    }

}
