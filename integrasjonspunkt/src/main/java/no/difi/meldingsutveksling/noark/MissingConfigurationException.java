package no.difi.meldingsutveksling.noark;

public class MissingConfigurationException extends RuntimeException {

    public static final String MISSING_CONFIGURATION = "Missing configuration: ";

    public MissingConfigurationException(String message) {
        super(MISSING_CONFIGURATION + message);
    }
}
