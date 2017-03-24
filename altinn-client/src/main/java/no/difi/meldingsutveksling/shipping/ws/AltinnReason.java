package no.difi.meldingsutveksling.shipping.ws;

/**
 * Class to contain error String messages from Altinn soap faults
 */
public class AltinnReason {
    private final Integer id;

    private final String message;
    private final String userId;
    private final String localized;

    AltinnReason(Integer id, String message, String userId, String localized) {
        this.id = id;
        this.message = message;
        this.userId = userId;
        this.localized = localized;
    }

    @Override
    public String toString() {
        return String.format("Reason: %s. LocalizedErrorMessage: %s. ErrorId: %d. UserId: %s", message, localized, id, userId);
    }

    public Integer getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getUserId() {
        return userId;
    }

    public String getLocalized() {
        return localized;
    }
}
