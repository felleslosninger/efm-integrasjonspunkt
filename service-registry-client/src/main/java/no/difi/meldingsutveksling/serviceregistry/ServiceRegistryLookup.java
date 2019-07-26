package no.difi.meldingsutveksling.serviceregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.nimbusds.jose.proc.BadJWSException;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.Process;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord.*;

@Service
@Slf4j
public class ServiceRegistryLookup {

    private final RestClient client;
    private final IntegrasjonspunktProperties properties;
    private final SasKeyRepository sasKeyRepository;
    private final ObjectMapper objectMapper;
    private final LoadingCache<String, String> skCache;
    private final LoadingCache<SRParameter, ServiceRecord> srCache;
    private final LoadingCache<SRParameter, List<ServiceRecord>> srsCache;
    private final LoadingCache<SRParameter, InfoRecord> irCache;

    @Autowired
    public ServiceRegistryLookup(RestClient client,
                                 IntegrasjonspunktProperties properties,
                                 SasKeyRepository sasKeyRepository,
                                 ObjectMapper objectMapper) {
        this.client = client;
        this.properties = properties;
        this.sasKeyRepository = sasKeyRepository;
        this.objectMapper = objectMapper;

        this.skCache = CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) throws Exception {
                        return loadSasKey();
                    }
                });

        this.srCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<SRParameter, ServiceRecord>() {
                    @Override
                    public ServiceRecord load(SRParameter key) throws Exception {
                        return loadServiceRecord(key);
                    }
                });

        this.srsCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<SRParameter, List<ServiceRecord>>() {
                    @Override
                    public List<ServiceRecord> load(SRParameter key) throws Exception {
                        return loadServiceRecords(key);
                    }
                });

        this.irCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<SRParameter, InfoRecord>() {
                    @Override
                    public InfoRecord load(SRParameter key) throws Exception {
                        return loadInfoRecord(key);
                    }
                });
    }

    public ServiceRecord getServiceRecord(SRParameter parameter) throws ServiceRegistryLookupException {
        try {
            return srCache.get(parameter);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ServiceRegistryLookupException) {
                throw (ServiceRegistryLookupException) e.getCause();
            } else {
                throw new MeldingsUtvekslingRuntimeException(e.getCause());
            }
        }
    }

    public ServiceRecord getServiceRecord(String identifier) throws ServiceRegistryLookupException {
        return getServiceRecord(SRParameter.builder(identifier).build());
    }

    public ServiceRecord getServiceRecord(String identifier, ServiceIdentifier serviceIdentifier) throws ServiceRegistryLookupException {
        return getServiceRecord(SRParameter.builder(identifier).build(), serviceIdentifier);
    }

    public ServiceRecord getServiceRecord(SRParameter parameter, ServiceIdentifier serviceIdentifier) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords;
        try {
            serviceRecords = srsCache.get(parameter);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ServiceRegistryLookupException) {
                throw (ServiceRegistryLookupException) e.getCause();
            } else {
                throw new MeldingsUtvekslingRuntimeException(e.getCause());
            }
        }
        return serviceRecords.stream()
                .filter(isServiceIdentifier(serviceIdentifier)).findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(String.format("Service record of type=%s not found for identifier=%s", serviceIdentifier, parameter.getIdentifier())));
    }

    public List<ServiceRecord> getServiceRecords(SRParameter parameter) {
        return srsCache.getUnchecked(parameter);
    }

    public List<ServiceRecord> getServiceRecords(String identifier) {
        return getServiceRecords(SRParameter.builder(identifier).build());
    }

    public boolean isInServiceRegistry(String identifier) {
        return !getServiceRecords(SRParameter.builder(identifier).build()).isEmpty();
    }

    public String getDocumentIdentifier(SRParameter parameter, Process process, DocumentType documentType) throws ServiceRegistryLookupException {
        return getDocumentIdentifier(parameter, process.getValue(), documentType);
    }

    public String getDocumentIdentifier(SRParameter parameter, String process, DocumentType documentType) throws ServiceRegistryLookupException {
        Set<ServiceRecord> serviceRecords = getServiceRecords(parameter, process);
        return serviceRecords.stream()
                .flatMap(r -> r.getDocumentTypes().stream())
                .filter(documentType::fitsDocumentIdentifier)
                .findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(
                        String.format("Standard not found for process '%s' and documentType '%s' for identifier '%s'",
                                process, documentType.getType(), parameter.getIdentifier())));

    }

    public ServiceRecord getServiceRecord(String identifier, String process, String documentType) throws ServiceRegistryLookupException {
        return getServiceRecord(SRParameter.builder(identifier).build(), process, documentType);
    }

    public ServiceRecord getServiceRecord(SRParameter parameter, String process, String documentType) throws ServiceRegistryLookupException {
        Set<ServiceRecord> serviceRecords = getServiceRecords(parameter, process);
        return serviceRecords.stream()
                .filter(hasDocumentType(documentType))
                .findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(String.format("Service record for identifier=%s with process=%s not found", parameter.getIdentifier(), process)));
    }

    private Set<ServiceRecord> getServiceRecords(SRParameter parameter, String process) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords = null;
        try {
            serviceRecords = srsCache.get(parameter);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ServiceRegistryLookupException) {
                throw (ServiceRegistryLookupException) e.getCause();
            } else {
                throw new MeldingsUtvekslingRuntimeException(e);
            }
        }
        return serviceRecords.stream()
                .filter(isProcess(process))
                .collect(Collectors.toSet());
    }

    private ServiceRecord loadServiceRecord(SRParameter SRParameter) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords = loadServiceRecords(SRParameter);

        Optional<ServiceRecord> serviceRecord = serviceRecords.stream()
                .filter(r -> r.getService().getIdentifier() == ServiceIdentifier.DPI)
                .findFirst();

        if (!serviceRecord.isPresent()) {
            String defaultProcess = properties.getArkivmelding().getDefaultProcess();
            serviceRecord = serviceRecords.stream()
                    .filter(r -> r.getProcess().equals(defaultProcess))
                    .findFirst();
        }

        if (!serviceRecord.isPresent()) {
            serviceRecord = serviceRecords.stream()
                    .filter(r -> r.getService().getIdentifier() == ServiceIdentifier.DPE)
                    .findFirst();
        }

        return serviceRecord.orElseThrow(() -> new ServiceRegistryLookupException(String.format("Could not find service record for receiver '%s'", SRParameter.getIdentifier())));
    }

    private List<ServiceRecord> loadServiceRecords(SRParameter SRParameter) throws ServiceRegistryLookupException {
        ServiceRecord[] serviceRecords;
        try {
            final String resource = client.getResource("identifier/" + SRParameter.getIdentifier(), SRParameter.getQuery());
            final DocumentContext documentContext = JsonPath.parse(resource, jsonPathConfiguration());
            serviceRecords = documentContext.read("$.serviceRecords", ServiceRecord[].class);
        } catch (HttpClientErrorException httpException) {
            byte[] errorBody = httpException.getResponseBodyAsByteArray();
            try {
                ErrorResponse error = objectMapper.readValue(errorBody, ErrorResponse.class);
                throw new ServiceRegistryLookupException(String.format("Caught exception when looking up service record with identifier %s, http status %s (%s): %s",
                        SRParameter.getIdentifier(), httpException.getStatusCode(), httpException.getStatusText(), error.getErrorDescription()), httpException);
            } catch (IOException e) {
                log.warn("Could not parse error response from service registry");
                throw new ServiceRegistryLookupException(String.format("Caught exception when looking up service record with identifier %s, http status: %s (%s)",
                        SRParameter.getIdentifier(), httpException.getStatusCode(), httpException.getStatusText()), httpException);
            }
        } catch (BadJWSException e) {
            log.error("Bad signature in service record response", e);
            throw new ServiceRegistryLookupException("Bad signature in service record response", e);
        }
        return Lists.newArrayList(serviceRecords);
    }

    /**
     * Method to fetch the info record for the given identifier
     *
     * @param identifier of the receiver
     * @return an {@link InfoRecord} for the respective identifier
     */
    public InfoRecord getInfoRecord(String identifier) {
        return irCache.getUnchecked(SRParameter.builder(identifier).build());
    }

    private InfoRecord loadInfoRecord(SRParameter SRParameter) throws ServiceRegistryLookupException {
        final String infoRecordString;
        try {
            infoRecordString = client.getResource("identifier/" + SRParameter.getIdentifier(), SRParameter.getQuery());
        } catch (BadJWSException e) {
            throw new ServiceRegistryLookupException("Bad signature in response from service registry", e);
        }
        final DocumentContext documentContext = JsonPath.parse(infoRecordString, jsonPathConfiguration());
        return documentContext.read("$.infoRecord", InfoRecord.class);
    }

    public String getSasKey() {
        // Single entry, key does not matter
        try {
            return skCache.get("");
        } catch (ExecutionException e) {
            throw new RuntimeException("An error occured when fetching SAS key", e.getCause());
        }
    }

    public void invalidateSasKey() {
        skCache.invalidateAll();
    }

    private String loadSasKey() throws SasKeyException {
        try {
            String sasKey = client.getResource("sastoken");
            // persist SAS key in case of ServiceRegistry down time
            sasKeyRepository.deleteAll();
            sasKeyRepository.save(SasKeyWrapper.of(sasKey));
            return sasKey;
        } catch (BadJWSException | RestClientException e) {
            log.error("An error occured when fetching SAS key from ServiceRegistry. Checking for persisted key..", e);
        }

        List<SasKeyWrapper> key = sasKeyRepository.findAll();
        if (key.isEmpty()) {
            throw new SasKeyException("No persisted SAS key found. Need to wait until connection with ServiceRegistry has been re-established..");
        }
        if (key.size() > 1) {
            log.error("SAS key repository should only ever have one entry - deleting all entries..");
            sasKeyRepository.deleteAll();
            throw new SasKeyException("Multiple keys found in repository, waiting for retry");
        }

        log.info("Loading locally persisted SAS key");
        return key.get(0).saskey;
    }

    private Configuration jsonPathConfiguration() {
        final Gson gson = new GsonBuilder().serializeNulls().create();
        return Configuration.defaultConfiguration().jsonProvider(new GsonJsonProvider(gson)).mappingProvider(new GsonMappingProvider());
    }
}
