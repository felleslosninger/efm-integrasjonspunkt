package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import java.util.List;

public class ServiceRecords {
    private List<ServiceRecord> serviceRecord;

    public ServiceRecords(List<ServiceRecord> serviceRecords) {
        this.serviceRecord = serviceRecords;
    }

    public List<ServiceRecord> getServiceRecord() {
        return serviceRecord;
    }
}
