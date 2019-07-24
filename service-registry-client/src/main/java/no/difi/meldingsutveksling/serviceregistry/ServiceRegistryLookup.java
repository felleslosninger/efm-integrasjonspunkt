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
import no.difi.meldingsutveksling.NextMoveConsts;
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
    private final LoadingCache<Parameters, ServiceRecord> srCache;
    private final LoadingCache<Parameters, List<ServiceRecord>> srsCache;
    private final LoadingCache<Parameters, InfoRecord> irCache;

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

    public ServiceRecord getServiceRecord(String identifier) throws ServiceRegistryLookupException {
        try {
            return srCache.get(new Parameters(identifier));
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ServiceRegistryLookupException) {
                throw (ServiceRegistryLookupException) e.getCause();
            } else {
                throw new MeldingsUtvekslingRuntimeException(e.getCause());
            }
        }
    }

    public ServiceRecord getServiceRecord(String identifier, ServiceIdentifier serviceIdentifier) throws ServiceRegistryLookupException {
        return getServiceRecord(identifier, serviceIdentifier, NextMoveConsts.DEFAULT_SECURITY_LEVEL);
    }

    public ServiceRecord getServiceRecord(String identifier, ServiceIdentifier serviceIdentifier, Integer securityLevel) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords;
        try {
            serviceRecords = srsCache.get(new Parameters(identifier, securityLevel));
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ServiceRegistryLookupException) {
                throw (ServiceRegistryLookupException) e.getCause();
            } else {
                throw new MeldingsUtvekslingRuntimeException(e.getCause());
            }
        }
        return serviceRecords.stream()
                .filter(isServiceIdentifier(serviceIdentifier)).findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(String.format("Service record of type=%s not found for identifier=%s", serviceIdentifier, identifier)));
    }

    public List<ServiceRecord> getServiceRecords(String identifier) {
        return srsCache.getUnchecked(new Parameters(identifier));
    }

    public List<ServiceRecord> getServiceRecords(String identifier, Integer securityLevel) {
        return srsCache.getUnchecked(new Parameters(identifier, securityLevel));
    }

    public boolean isInServiceRegistry(String identifier) {
        return !getServiceRecords(identifier).isEmpty();
    }

    public String getDocumentIdentifier(String identifier, Process process, DocumentType documentType) throws ServiceRegistryLookupException {
        return getDocumentIdentifier(identifier, process.getValue(), documentType);
    }

    public String getDocumentIdentifier(String identifier, String process, DocumentType documentType) throws ServiceRegistryLookupException {
        Set<ServiceRecord> serviceRecords = getServiceRecords(identifier, process, NextMoveConsts.DEFAULT_SECURITY_LEVEL);
        return serviceRecords.stream()
                .flatMap(r -> r.getDocumentTypes().stream())
                .filter(documentType::fitsDocumentIdentifier)
                .findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(
                        String.format("Standard not found for process '%s' and documentType '%s' for identifier '%s'",
                                process, documentType.getType(), identifier)));

    }

    public ServiceRecord getServiceRecord(String identifier, String process, String documentType) throws ServiceRegistryLookupException {
        return getServiceRecord(identifier, process, documentType, NextMoveConsts.DEFAULT_SECURITY_LEVEL);
    }

    public ServiceRecord getServiceRecord(String identifier, String process, String documentType, Integer securityLevel) throws ServiceRegistryLookupException {
        Set<ServiceRecord> serviceRecords = getServiceRecords(identifier, process, securityLevel);
        return serviceRecords.stream()
                .filter(hasDocumentType(documentType))
                .findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(String.format("Service record for identifier=%s with process=%s not found", identifier, process)));
    }

    private Set<ServiceRecord> getServiceRecords(String identifier, String process, Integer securityLevel) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords = null;
        try {
            serviceRecords = srsCache.get(new Parameters(identifier, securityLevel));
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

    private ServiceRecord loadServiceRecord(Parameters parameters) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords = loadServiceRecords(parameters);

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

        return serviceRecord.orElseThrow(() -> new ServiceRegistryLookupException(String.format("Could not find service record for receiver '%s'", parameters.getIdentifier())));
    }

    private List<ServiceRecord> loadServiceRecords(Parameters parameters) throws ServiceRegistryLookupException {
        ServiceRecord[] serviceRecords;
        try {
            final String resource = client.getResource("identifier/" + parameters.getIdentifier(), parameters.getQuery());
            final DocumentContext documentContext = JsonPath.parse(resource, jsonPathConfiguration());
            serviceRecords = documentContext.read("$.serviceRecords", ServiceRecord[].class);
        } catch (HttpClientErrorException httpException) {
            byte[] errorBody = httpException.getResponseBodyAsByteArray();
            try {
                ErrorResponse error = objectMapper.readValue(errorBody, ErrorResponse.class);
                throw new ServiceRegistryLookupException(String.format("Caught exception when looking up service record with identifier %s, http status %s (%s): %s",
                        parameters.getIdentifier(), httpException.getStatusCode(), httpException.getStatusText(), error.getErrorDescription()), httpException);
            } catch (IOException e) {
                log.warn("Could not parse error response from service registry");
                throw new ServiceRegistryLookupException(String.format("Caught exception when looking up service record with identifier %s, http status: %s (%s)",
                        parameters.getIdentifier(), httpException.getStatusCode(), httpException.getStatusText()), httpException);
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
        return irCache.getUnchecked(new Parameters(identifier));
    }

    private InfoRecord loadInfoRecord(Parameters parameters) throws ServiceRegistryLookupException {
        final String infoRecordString;
        try {
            infoRecordString = client.getResource("identifier/" + parameters.getIdentifier(), parameters.getQuery());
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
