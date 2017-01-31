package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

public class ServiceRecordObjectMother {
    public static ServiceRecord createDPVServiceRecord(String serviceIdentifier) {
        return new ServiceRecord(ServiceIdentifier.DPV.fullname(), serviceIdentifier, "", "http://localhost");
    }
}
