package no.difi.meldingsutveksling.serviceregistry.common;

public class ServiceDiscoveryException extends RuntimeException {
    public ServiceDiscoveryException(Exception e) {
        super(e);
    }

}