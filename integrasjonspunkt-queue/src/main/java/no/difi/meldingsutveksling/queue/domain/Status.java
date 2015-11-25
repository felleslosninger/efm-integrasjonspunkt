package no.difi.meldingsutveksling.queue.domain;

public enum Status {
    NEW,
    DONE,
    RETRY,
    ERROR;

    public static Status statusFromString(String status) {
        for (Status statEnum : Status.values()) {
            if (statEnum.name().equals(status)) {
                return statEnum;
            }
        }
        throw new IllegalArgumentException("Queue status not found");
    }
}
