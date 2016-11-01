package no.difi.meldingsutveksling.serviceregistry;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.*;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import java.util.concurrent.TimeUnit;

public class ServiceRegistryLookup {
    private final RestClient client;
    private final LoadingCache<String, ServiceRecord> srCache;
    private final LoadingCache<String, InfoRecord> irCache;

    public ServiceRegistryLookup(RestClient client) {
        this.client = client;

        this.srCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<String, ServiceRecord>() {
                    @Override
                    public ServiceRecord load(String key) throws Exception {
                        return loadServiceRecord(key);
                    }
                });

        this.irCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<String, InfoRecord>() {
                    @Override
                    public InfoRecord load(String key) throws Exception {
                        return loadInfoRecord(key);
                    }
                });
    }

    /**
     * Method to find out which transport channel to use to send messages to given organization
     * @param identifier of the receiver
     * @return a ServiceRecord if found. Otherwise an empty ServiceRecord is returned.
     */
    public ServiceRecord getServiceRecord(String identifier) {
        return srCache.getUnchecked(identifier);
    }

    private ServiceRecord loadServiceRecord(String identifier) {
        final String serviceRecords = client.getResource("identifier/" + identifier);
        final DocumentContext documentContext = JsonPath.parse(serviceRecords, jsonPathConfiguration());
        ServiceRecord serviceRecord = documentContext.read("$.serviceRecord", ServiceRecord.class);
        if (serviceRecord != null) {
            return serviceRecord;
        }
        return ServiceRecord.EMPTY;
    }

    /**
     * Method to fetch the info record for the given identifier
     * @param identifier of the receiver
     * @return an {@link InfoRecord} for the respective identifier
     */
    public InfoRecord getInfoRecord(String identifier) {
        return irCache.getUnchecked(identifier);
    }

    private InfoRecord loadInfoRecord(String identifier) {
        final String infoRecordString = client.getResource("identifier/" + identifier);
        final DocumentContext documentContext = JsonPath.parse(infoRecordString, jsonPathConfiguration());
        return documentContext.read("$.infoRecord", InfoRecord.class);
    }

    private int getNumberOfServiceRecords(DocumentContext documentContext) {
        return documentContext.read("$.serviceRecords.length()");
    }

    private Configuration jsonPathConfiguration() {
        final Gson gson = new GsonBuilder().serializeNulls().create();
        return Configuration.defaultConfiguration().jsonProvider(new GsonJsonProvider(gson));
    }
}
