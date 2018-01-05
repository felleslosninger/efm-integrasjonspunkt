package no.difi.meldingsutveksling.serviceregistry;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.nimbusds.jose.proc.BadJWSException;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.Notification;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord.isServiceIdentifier;

@Service
public class ServiceRegistryLookup {
    private final RestClient client;
    private IntegrasjonspunktProperties properties;
    private final LoadingCache<Parameters, ServiceRecord> srCache;
    private final LoadingCache<Parameters, List<ServiceRecord>> srsCache;
    private final LoadingCache<Parameters, InfoRecord> irCache;
    private final Supplier<String> sasTokenSupplier;

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    @Autowired
    public ServiceRegistryLookup(RestClient client, IntegrasjonspunktProperties properties) {
        this.client = client;
        this.properties = properties;

        this.sasTokenSupplier = Suppliers.memoizeWithExpiration(loadSasToken(), 1, TimeUnit.MINUTES);

        this.srCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<Parameters, ServiceRecord>() {
                    @Override
                    public ServiceRecord load(Parameters key) throws Exception {
                        return loadServiceRecord(key);
                    }
                });

        this.srsCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<Parameters, List<ServiceRecord>>() {
                    @Override
                    public List<ServiceRecord> load(Parameters key) throws Exception {
                        return loadServiceRecords(key);
                    }
                });

        this.irCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<Parameters, InfoRecord>() {
                    @Override
                    public InfoRecord load(Parameters key) throws Exception {
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
        Notification notification = properties.isVarslingsplikt()? Notification.OBLIGATED : Notification.NOT_OBLIGATED;
        return srCache.getUnchecked(new Parameters(identifier, notification));
    }

    public Optional<ServiceRecord> getServiceRecord(String identifier, ServiceIdentifier serviceIdentifier) {
        Notification notification = properties.isVarslingsplikt()? Notification.OBLIGATED : Notification.NOT_OBLIGATED;
        List<ServiceRecord> serviceRecords = srsCache.getUnchecked(new Parameters(identifier, notification));
        return serviceRecords.stream().filter(isServiceIdentifier(serviceIdentifier)).findFirst();
    }

    public List<ServiceRecord> getServiceRecords(String identifier) {
        Notification notification = properties.isVarslingsplikt()? Notification.OBLIGATED : Notification.NOT_OBLIGATED;
        return srsCache.getUnchecked(new Parameters(identifier, notification));
    }

    private ServiceRecord loadServiceRecord(Parameters parameters) {
        ServiceRecord serviceRecord = ServiceRecord.EMPTY;
        try {
            final String serviceRecords = client.getResource("identifier/" + parameters.getIdentifier(), parameters.getQuery());
            final DocumentContext documentContext = JsonPath.parse(serviceRecords, jsonPathConfiguration());
            serviceRecord = documentContext.read("$.serviceRecord", ServiceRecord.class);
        } catch(HttpClientErrorException httpException) {
            if (Arrays.asList(HttpStatus.NOT_FOUND, HttpStatus.UNAUTHORIZED).contains(httpException.getStatusCode())) {
                logger.warn("RestClient returned {} when looking up service record with identifier {}",
                        httpException.getStatusCode(), parameters, httpException);
            } else {
                throw new ServiceRegistryLookupException(String.format("RestClient threw exception when looking up service record with identifier %s", parameters), httpException);
            }
        } catch (BadJWSException e) {
            logger.error("Bad signature in service record response", e);
            throw new ServiceRegistryLookupException("Bad signature in service record response", e);
        }
        return serviceRecord;
    }

    private List<ServiceRecord> loadServiceRecords(Parameters parameters) {
        ServiceRecord[] serviceRecords = {};
        try {
            final String resource = client.getResource("identifier/" + parameters.getIdentifier(), parameters.getQuery());
            final DocumentContext documentContext = JsonPath.parse(resource, jsonPathConfiguration());
            serviceRecords = documentContext.read("$.serviceRecords", ServiceRecord[].class);
        } catch(HttpClientErrorException httpException) {
            if (Arrays.asList(HttpStatus.NOT_FOUND, HttpStatus.UNAUTHORIZED).contains(httpException.getStatusCode())) {
                logger.warn("RestClient returned {} when looking up service record with identifier {}",
                        httpException.getStatusCode(), parameters, httpException);
            } else {
                throw new ServiceRegistryLookupException(String.format("RestClient threw exception when looking up service record with identifier %s", parameters), httpException);
            }
        } catch (BadJWSException e) {
            logger.error("Bad signature in service record response", e);
            throw new ServiceRegistryLookupException("Bad signature in service record response", e);
        }
        return Lists.newArrayList(serviceRecords);
    }

    /**
     * Method to fetch the info record for the given identifier
     * @param identifier of the receiver
     * @return an {@link InfoRecord} for the respective identifier
     */
    public InfoRecord getInfoRecord(String identifier) {
        Notification notification = properties.isVarslingsplikt()? Notification.OBLIGATED : Notification.NOT_OBLIGATED;
        return irCache.getUnchecked(new Parameters(identifier, notification));
    }

    private InfoRecord loadInfoRecord(Parameters parameters) {
        final String infoRecordString;
        try {
            infoRecordString = client.getResource("identifier/" + parameters.getIdentifier(), parameters.getQuery());
        } catch (BadJWSException e) {
            throw new ServiceRegistryLookupException("Bad signature in response from service registry", e);
        }
        final DocumentContext documentContext = JsonPath.parse(infoRecordString, jsonPathConfiguration());
        return documentContext.read("$.infoRecord", InfoRecord.class);
    }

    private int getNumberOfServiceRecords(DocumentContext documentContext) {
        return documentContext.read("$.serviceRecords.length()");
    }

    /**
     * Method to fetch SAS token from Service Registry.
     * Token is cached with 1 minute timeout.
     *
     * @return SAS token
     */
    public String getSasToken() {
        return sasTokenSupplier.get();
    }

    private Supplier<String> loadSasToken() {
        return () -> {
            try {
                return client.getResource("sastoken");
            } catch (BadJWSException e) {
                throw new ServiceRegistryLookupException("Bad signature in response from service registry", e);
            }
        };
    }

    private Configuration jsonPathConfiguration() {
        final Gson gson = new GsonBuilder().serializeNulls().create();
        return Configuration.defaultConfiguration().jsonProvider(new GsonJsonProvider(gson));
    }
}
