package no.difi.meldingsutveksling.serviceregistry;

/**
 * Exception indicating that Service Registry, or something Service Registry depends on, is temporarily unavailable.
 * <p>
 * Unchecked by design: callers that treat {@link ServiceRegistryLookupException} as a permanent failure must not
 * swallow this. It should propagate so that REST endpoints respond with 503 and queued messages are redelivered.
 */
public class ServiceRegistryUnavailableException extends RuntimeException {

    public ServiceRegistryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
