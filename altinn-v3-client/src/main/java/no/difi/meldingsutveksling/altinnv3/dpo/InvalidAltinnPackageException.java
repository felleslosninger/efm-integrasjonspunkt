package no.difi.meldingsutveksling.altinnv3.dpo;

/**
 * Thrown when a downloaded file can never be read as an Altinn package (empty file, not a zip, missing SBD).
 * Retrying the download will not help, so the file should be discarded.
 */
public class InvalidAltinnPackageException extends RuntimeException {

    public InvalidAltinnPackageException(String message) {
        super(message);
    }

    public InvalidAltinnPackageException(String message, Throwable cause) {
        super(message, cause);
    }

}
