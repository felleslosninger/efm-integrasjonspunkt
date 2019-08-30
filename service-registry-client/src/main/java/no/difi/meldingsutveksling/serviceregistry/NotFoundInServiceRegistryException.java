package no.difi.meldingsutveksling.serviceregistry;

public class NotFoundInServiceRegistryException extends ServiceRegistryLookupException {

    public NotFoundInServiceRegistryException(String identifier) {
        super(String.format("Identifier %s not found in Service Registry", identifier));
    }
}
