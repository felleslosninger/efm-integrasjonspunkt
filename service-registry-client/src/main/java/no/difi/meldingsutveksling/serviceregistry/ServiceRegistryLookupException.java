package no.difi.meldingsutveksling.serviceregistry;

/**
 * Exception indicating that something is technically wrong with looking up service record in Service Registry
 */
public class ServiceRegistryLookupException extends Exception {

    ServiceRegistryLookupException(String format, Exception e) {
        super(format, e);
    }

    ServiceRegistryLookupException(String s) {
        super(s);
    }

    ServiceRegistryLookupException(Throwable t) {
        super(t);
    }
}
