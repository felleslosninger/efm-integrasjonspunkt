package no.difi.meldingsutveksling.serviceregistry;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.Notification;

public class Parameters {
    private String identifier;
    private String process;
    private Notification notification;

    public Parameters(String identifier) {
        this.identifier = identifier;
    }

    public Parameters(String identifier, String process, Notification notification) {
        this.identifier = identifier;
        this.process = process;
        this.notification = notification;
    }

    public Parameters(String identifier, Notification notification) {
        this.identifier = identifier;
        this.notification = notification;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getProcess() {
        return process;
    }

    @Override
    @SuppressWarnings({"squid:S00122", "squid:S1067"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameters that = (Parameters) o;
        return Objects.equal(identifier, that.identifier)
                && Objects.equal(process, that.process)
                && notification == that.notification;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(identifier, process, notification);
    }

    public String getQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append(notification.createQuery());
        if (process != null) {
            sb.append("&process=").append(process);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("identifier", identifier)
                .add("process", process)
                .add("notification", notification)
                .toString();
    }
}
