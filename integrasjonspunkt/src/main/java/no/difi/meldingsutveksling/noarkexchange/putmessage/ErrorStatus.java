package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;

/**
 * Created by mfhoel on 26.10.15.
 */
public enum ErrorStatus {
    MISSING_RECIPIENT(1, "Request is missing recipient party number"),
    MISSING_SENDER(2, "Integrasjonspunkt must know sender party number. Please configure: " + IntegrasjonspunktConfig.KEY_ORGANISATION_NUMBER),
    CANNOT_RECIEVE(3, "Recipient must have a valid certificate in the adresseregister used");

    public final Integer id;
    public final String message;

    ErrorStatus(Integer id, String message) {
        this.id = id;
        this.message = message;
    }

    @Override
    public String toString() {
        return "ErrorStatus{" +
                "id=" + id +
                ", message='" + message + '\'' +
                '}';
    }
}
