package no.difi.meldingsutveksling.eventlog;

import no.difi.meldingsutveksling.domain.ProcessState;

import java.util.Date;
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
    private String formattedDate;
    private ProcessState processState;

    private String exceptionMessage;
    private String message;

    private String hubConversationId;
    private String arkiveConversationId;
    private String jpId;

    public Event(ProcessState processState) {
        this.processState = processState;
    }

    public Event() {
        this(System.currentTimeMillis(), UUID.randomUUID());
    }

    public Event(long timeStamp, UUID uuid) {
        this.timeStamp = timeStamp;
        this.uuid = uuid;
        formattedDate = new Date(timeStamp).toString();

    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public Event setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
        return this;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getTimeStamp() {
        return timeStamp;
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

    public String getFormattedDate() {
        return formattedDate;
    }

    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }

    public void setProcessState(ProcessState processState) {
        this.processState = processState;
    }

    public String getMessage() {
        return message;
    }

    public Event setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getHubConversationId() {
        return hubConversationId;
    }

    public Event setHubConversationId(String hubConversationId) {
        this.hubConversationId = hubConversationId;
        return this;
    }

    public String getArkiveConversationId() {
        return arkiveConversationId;
    }

    public Event setArkiveConversationId(String arkiveConversationId) {
        this.arkiveConversationId = arkiveConversationId;
        return this;
    }

    public String getJpId() {
        return jpId;
    }

    public Event setJpId(String jpId) {
        this.jpId = jpId;
        return this;
    }

    @Override
    public String toString() {
        return "Event{" +
                "uuid=" + uuid +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", timeStamp=" + timeStamp +
                ", formattedDate='" + formattedDate + '\'' +
                ", processState=" + processState +
                ", exceptionMessage='" + exceptionMessage + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public static Event errorEvent(String sender, String receiever, ProcessState state, String errorMessage, String payLoad) {
        Event event = new Event();
        event.setSender(sender);
        event.setReceiver(receiever);
        event.setExceptionMessage(errorMessage);
        event.setProcessStates(state);
        event.setMessage(payLoad);
        return event;
    }


}
