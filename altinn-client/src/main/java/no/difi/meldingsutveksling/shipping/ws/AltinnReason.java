package no.difi.meldingsutveksling.shipping.ws;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Class to contain error String messages from Altinn soap faults
 */
@RequiredArgsConstructor
@Getter
public class AltinnReason {

    private final Integer id;
    private final String message;
    private final String userId;
    private final String localized;

    @Override
    public String toString() {
        return String.format("Reason: %s. LocalizedErrorMessage: %s. ErrorId: %d. UserId: %s", message, localized, id, userId);
    }

}
