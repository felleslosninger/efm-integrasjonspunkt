package no.difi.meldingsutveksling.serviceregistry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

public class ServiceRegistryLookup {
    private final RestClient client;

    public ServiceRegistryLookup(RestClient client) {
        this.client = client;
    }

    public ServiceRecord getPrimaryServiceRecord(String orgnumber) {
        final String serviceRecords = client.getResource(orgnumber);
        if (getNumberOfServiceRecords(serviceRecords) == 1) {
            return JsonPath.parse(serviceRecords, jsonPathConfiguration()).read("$.serviceRecords[0].serviceRecord", ServiceRecord.class);
        }

        final String primaryServiceIdentifier = JsonPath.read(serviceRecords, "$.infoRecord.primaryServiceIdentifier");
        if (primaryServiceIdentifier == null) {
            return ServiceRecord.EMPTY;
        } else {
            final JsonArray res = JsonPath.parse(serviceRecords, jsonPathConfiguration()).read("$.serviceRecords[?(@.serviceRecord.serviceIdentifier == $.infoRecord.primaryServiceIdentifier)].serviceRecord");
            return new Gson().fromJson(res.get(0), ServiceRecord.class);
        }

    }

    private int getNumberOfServiceRecords(String serviceRecords) {
        return JsonPath.read(serviceRecords, "$.serviceRecords.length()");
    }

    private Configuration jsonPathConfiguration() {
        final Gson gson = new GsonBuilder().serializeNulls().create();
        return Configuration.defaultConfiguration().jsonProvider(new GsonJsonProvider(gson));
    }
}
