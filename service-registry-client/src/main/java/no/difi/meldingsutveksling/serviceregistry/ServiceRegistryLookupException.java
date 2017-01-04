package no.difi.meldingsutveksling.serviceregistry;

import org.springframework.web.client.HttpClientErrorException;

/**
 * Exception indicating that something is technically wrong with looking up service record in Service Registry
 */
class ServiceRegistryLookupException extends RuntimeException {
    ServiceRegistryLookupException(String format, HttpClientErrorException httpException) {
        super(format, httpException);
    }
}
