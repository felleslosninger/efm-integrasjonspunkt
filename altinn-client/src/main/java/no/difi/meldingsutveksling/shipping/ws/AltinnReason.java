package no.difi.meldingsutveksling.shipping.ws;

/**
 * Class to contain error String messages from Altinn soap faults
 */
public class AltinnReason {
    private final Integer id;

    private final String message;
    private final String userId;

    AltinnReason(Integer id, String message, String userId) {
        this.id = id;
        this.message = message;
        this.userId = userId;
    }

    @Override
    public String toString() {
        return String.format("Reason: %s. ErrorId: %d. UserId: %s", message, id, userId);
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
}
