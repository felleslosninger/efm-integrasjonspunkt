package no.difi.meldingsutveksling.eventlog;

import java.sql.Timestamp;
import java.util.UUID;


import java.util.UUID;

public class Event {
    private UUID uuid;
    private String sender;
    private String receiver;
    private long timeStamp;
    private ProcessState processState;
    private Exception exception;

    public Exception getException() {
        return exception;
    }

    public Event setException(Exception exception) {
        this.exception = exception;
        return this;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Event setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public Event setTimeStamp(long timeStamp) {
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


    public String getSender() {
        return sender;
    }

    public Event setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public String getReceiver() {
        return receiver;
    }

    public Event setReceiver(String receiver) {
        this.receiver = receiver;
        return this;
    }

    public String toString() {
        String toReturn= "UUID: " + uuid +" time: "+timeStamp+" sender: " +sender+" reciever: "+ receiver+ " process state: "+ processState;
        return toReturn;
    }

}
