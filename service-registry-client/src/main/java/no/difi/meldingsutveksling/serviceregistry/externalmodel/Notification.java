package no.difi.meldingsutveksling.serviceregistry.externalmodel;

/**
 * Used to specify whether recipient is obligated to be notified when sending a message
 * This can determine the outcome of Service registry lookup.
 */
public enum Notification {
    OBLIGATED, NOT_OBLIGATED;

    public String createQuery() {
        return String.format("%s=%s", Notification.class.getSimpleName().toLowerCase(), this.name().toLowerCase());
    }
}
