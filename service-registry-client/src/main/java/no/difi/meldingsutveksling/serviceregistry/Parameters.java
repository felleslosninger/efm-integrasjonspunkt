package no.difi.meldingsutveksling.serviceregistry;

import no.difi.meldingsutveksling.serviceregistry.externalmodel.Notification;

import java.util.Objects;

public class Parameters {
    private String identifier;
    private Notification notification;

    public Parameters(String identifier) {
        this.identifier = identifier;
    }

    public Parameters(String identifier, Notification notification) {
        this.identifier = identifier;
        this.notification = notification;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    @SuppressWarnings({"squid:S00122", "squid:S1067"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameters that = (Parameters) o;
        return Objects.equals(identifier, that.identifier)
                && notification == that.notification;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, notification);
    }

    String getQuery() {
        if (notification != null) {
            return notification.createQuery();
        }
        return null;
    }

    @Override
    public String toString() {
        return "Parameters{" +
                "identifier='" + identifier + '\'' +
                ", notification=" + notification +
                '}';
    }
}
