package no.difi.meldingsutveksling.noarkexchange;

public class MissingConfigurationException extends RuntimeException {

    public static final String MISSING_CONFIGURATION = "Missing configuration: ";

    public MissingConfigurationException(String message) {
        super(MISSING_CONFIGURATION + message);
    }
}
