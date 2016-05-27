package no.difi.meldingsutveksling.serviceregistry.externalmodel;

public class ServiceDiscoveryException extends RuntimeException {
    public ServiceDiscoveryException(Exception e) {
        super(e);
    }

}