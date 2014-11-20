package no.difi.meldingsutveksling.eventlog;

import java.util.UUID;

/**
 * A class that represents an Event
 *
 * @author Glenn Bech
 */

public class Event {
    private UUID uuid;
    private String sender;
    private String receiver;
    private long timeStamp;
    private ProcessState processState;

    private Exception exceptionMessage;
    private String message;

    public Exception getExceptionMessage() {
        return exceptionMessage;
    }

    public Event setExceptionMessage(Exception exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
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

    @Override
    public String toString() {
        return "Event{" +
                "uuid=" + uuid +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", timeStamp=" + timeStamp +
                ", processState=" + processState +
                ", exceptionMessage='" + exceptionMessage + '\'' +
                '}';
    }

    public String getMessage() {
        return message;
    }

    public Event setMessage(String message) {
        this.message = message;
        return this;
    }
}
