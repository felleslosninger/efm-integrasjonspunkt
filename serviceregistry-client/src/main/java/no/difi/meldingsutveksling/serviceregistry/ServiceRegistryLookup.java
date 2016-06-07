package no.difi.meldingsutveksling.serviceregistry;

import com.jayway.jsonpath.JsonPath;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

public class ServiceRegistryLookup {
    private final RestClient client;

    public ServiceRegistryLookup(RestClient client) {
        this.client = client;
    }

    public ServiceRecord getPrimaryServiceRecord(String orgnumber) {
        final String serviceRecords = client.getResource(orgnumber);
        final String primaryServiceRecord = JsonPath.read(serviceRecords, "$.infoRecord.primaryServiceIdentifier");
        if (primaryServiceRecord == null) {
            return ServiceRecord.EMPTY;
        }

        return new ServiceRecord();
    }
}
