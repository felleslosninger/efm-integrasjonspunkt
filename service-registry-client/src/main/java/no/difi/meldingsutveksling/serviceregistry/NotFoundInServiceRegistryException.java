package no.difi.meldingsutveksling.serviceregistry;

public class NotFoundInServiceRegistryException extends ServiceRegistryLookupException {

    public NotFoundInServiceRegistryException(SRParameter parameter) {
        super("Service record not found for parameters: "+parameter);
    }
}
