package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;

/**
 * Enum to contain error messages for the logs and for the end user
 */
public enum ErrorStatus {
    MISSING_RECIPIENT(1, "Request is missing recipient party number"),
    MISSING_SENDER(2, "Integrasjonspunkt must know sender party number. Please configure: " + IntegrasjonspunktConfig.KEY_ORGANISATION_NUMBER),
    CANNOT_RECIEVE(3, "Recipient must have a valid certificate in the adresseregister used");

    public static final String TEKNISK_FEIL = "Teknisk feil";
    public static final String MOTTAKENDE_ORGANISASJON_KAN_IKKE_MOTTA_MELDINGER = "Mottakende organisasjon kan ikke motta meldinger";

    public final Integer id;
    public final String technicalMessage;

    ErrorStatus(Integer id, String technicalMessage) {
        this.id = id;
        this.technicalMessage = technicalMessage;
    }

    public String enduserErrorMessage() {
        switch(this) {
            case CANNOT_RECIEVE:
                return MOTTAKENDE_ORGANISASJON_KAN_IKKE_MOTTA_MELDINGER;
            default:
                return TEKNISK_FEIL;
        }
    }

    @Override
    public String toString() {
        return "ErrorStatus{" +
                "id=" + id +
                ", technicalMessage='" + technicalMessage + '\'' +
                '}';
    }
}
