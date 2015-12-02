package no.difi.meldingsutveksling.queue.domain;

public enum Status {
    NEW,
    DONE,
    RETRY,
    ERROR;

    public static Status statusFromString(String status) {
        return Status.valueOf(status);
    }
}
