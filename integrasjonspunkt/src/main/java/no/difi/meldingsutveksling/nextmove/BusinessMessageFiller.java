package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

public interface BusinessMessageFiller<T extends BusinessMessage<T>> {

    void setDefaults(BusinessMessage<?> message, ServiceRecord serviceRecord);

    Class<T> getType();

}
