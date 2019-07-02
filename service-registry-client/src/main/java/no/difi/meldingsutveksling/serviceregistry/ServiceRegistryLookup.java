package no.difi.meldingsutveksling.serviceregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.proc.BadJWSException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.Process;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.IdentifierResource;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRegistryLookup {

    public static final String CACHE_GET_SAS_KEY = "getSasKey";
    public static final String CACHE_GET_INFO_RECORD = "getInfoRecord";
    public static final String CACHE_GET_SERVICE_RECORD = "getServiceRecord";
    public static final String CACHE_GET_SERVICE_RECORDS = "getServiceRecords";

    private final RestClient client;
    private final IntegrasjonspunktProperties properties;
    private final SasKeyRepository sasKeyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Method to find out which transport channel to use to send messages to given organization
     *
     * @param identifier of the receiver
     * @return a ServiceRecord if found. Otherwise an empty ServiceRecord is returned.
     */
    @Cacheable(CACHE_GET_SERVICE_RECORD)
    public ServiceRecord getServiceRecord(String identifier) throws ServiceRegistryLookupException {
        return loadServiceRecord(new Parameters(identifier));
    }

    @Cacheable(CACHE_GET_SERVICE_RECORD)
    public ServiceRecord getServiceRecord(String identifier, ServiceIdentifier serviceIdentifier) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords = loadServiceRecords(new Parameters(identifier));
        return serviceRecords.stream()
                .filter(isServiceIdentifier(serviceIdentifier)).findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(String.format("Service record of type=%s not found for identifier=%s", serviceIdentifier, identifier)));
    }

    @Cacheable(CACHE_GET_SERVICE_RECORDS)
    public List<ServiceRecord> getServiceRecords(String identifier) {
        try {
            return loadServiceRecords(new Parameters(identifier));
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public boolean isInServiceRegistry(String identifier) {
        return !getServiceRecords(identifier).isEmpty();
    }

    public String getDocumentIdentifier(String identifier, Process process, DocumentType documentType) throws ServiceRegistryLookupException {
        return getDocumentIdentifier(identifier, process.getValue(), documentType);
    }

    public String getDocumentIdentifier(String identifier, String process, DocumentType documentType) throws ServiceRegistryLookupException {
        Set<ServiceRecord> serviceRecords = getServiceRecords(identifier, process);
        return serviceRecords.stream()
                .flatMap(r -> r.getDocumentTypes().stream())
                .filter(documentType::fitsDocumentIdentifier)
                .findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(
                        String.format("Standard not found for process '%s' and documentType '%s' for identifier '%s'",
                                process, documentType.getType(), identifier)));

    }

    public ServiceRecord getServiceRecord(String identifier, String process, String documentType) throws ServiceRegistryLookupException {
        Set<ServiceRecord> serviceRecords = getServiceRecords(identifier, process);
        return serviceRecords.stream()
                .filter(hasDocumentType(documentType))
                .findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(String.format("Service record for identifier=%s with process=%s not found", identifier, process)));
    }

    private Set<ServiceRecord> getServiceRecords(String identifier, String process) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords = loadServiceRecords(new Parameters(identifier));
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
        return loadIdentifierResource(parameters).getServiceRecords();
    }

    /**
     * Method to fetch the info record for the given identifier
     *
     * @param identifier of the receiver
     * @return an {@link InfoRecord} for the respective identifier
     */
    @Cacheable(CACHE_GET_INFO_RECORD)
    public InfoRecord getInfoRecord(String identifier) {
        try {
            return loadInfoRecord(new Parameters(identifier));
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    private InfoRecord loadInfoRecord(Parameters parameters) throws ServiceRegistryLookupException {
        return loadIdentifierResource(parameters).getInfoRecord();
    }

    private IdentifierResource loadIdentifierResource(Parameters parameters) throws ServiceRegistryLookupException {
        String identifierResourceString = getIdentifierResourceString(parameters);

        try {
            return objectMapper.readValue(identifierResourceString, IdentifierResource.class);
        } catch (IOException e) {
            throw new ServiceRegistryLookupException(
                    String.format("Parsing response as a IdentifierResource JSON object failed. Content is: %s", identifierResourceString)
                    , e);
        }
    }

    private String getIdentifierResourceString(Parameters parameters) throws ServiceRegistryLookupException {
        try {
            return client.getResource("identifier/" + parameters.getIdentifier(), parameters.getQuery());
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
            throw new ServiceRegistryLookupException("Bad signature in response from service registry", e);
        }
    }

    @Cacheable(CACHE_GET_SAS_KEY)
    public String getSasKey() {
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
}
