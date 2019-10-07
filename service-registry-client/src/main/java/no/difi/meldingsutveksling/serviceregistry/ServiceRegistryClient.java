package no.difi.meldingsutveksling.serviceregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.nimbusds.jose.proc.BadJWSException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.IdentifierResource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRegistryClient {

    public static final String CACHE_LOAD_IDENTIFIER_RESOURCE = "loadIdentifierResource";
    public static final String CACHE_GET_SAS_KEY = "getSasKey";

    private final RestClient client;
    private final SasKeyRepository sasKeyRepository;
    private final ObjectMapper objectMapper;

    @Cacheable(CACHE_LOAD_IDENTIFIER_RESOURCE)
    public IdentifierResource loadIdentifierResource(SRParameter parameter) throws ServiceRegistryLookupException {
        return loadIdentifierResource(parameter, null);
    }

    @Cacheable(CACHE_LOAD_IDENTIFIER_RESOURCE)
    public IdentifierResource loadIdentifierResource(SRParameter parameter, String processId) throws ServiceRegistryLookupException {
        String identifierResourceString = getIdentifierResourceString(parameter, processId);

        try {
            return objectMapper.readValue(identifierResourceString, IdentifierResource.class);
        } catch (IOException e) {
            throw new ServiceRegistryLookupException(
                    String.format("Parsing response as a IdentifierResource JSON object failed. Content is: %s", identifierResourceString)
                    , e);
        }
    }

    private String getIdentifierResourceString(SRParameter parameter, String processId) throws ServiceRegistryLookupException {
        try {
            if (!Strings.isNullOrEmpty(processId)) {
                return client.getResource("identifier/" + parameter.getIdentifier() + "/process/" + processId, parameter.getQuery());
            } else {
                return client.getResource("identifier/" + parameter.getIdentifier(), parameter.getQuery());
            }
        } catch (HttpClientErrorException httpException) {
            if (httpException.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NotFoundInServiceRegistryException(parameter.getIdentifier());
            }
            byte[] errorBody = httpException.getResponseBodyAsByteArray();
            try {
                ErrorResponse error = objectMapper.readValue(errorBody, ErrorResponse.class);
                throw new ServiceRegistryLookupException(String.format("Caught exception when looking up service record with identifier %s, http status %s (%s): %s",
                        parameter.getIdentifier(), httpException.getStatusCode(), httpException.getStatusText(), error.getErrorDescription()), httpException);
            } catch (IOException e) {
                log.warn("Could not parse error response from service registry");
                throw new ServiceRegistryLookupException(String.format("Caught exception when looking up service record with identifier %s, http status: %s (%s)",
                        parameter.getIdentifier(), httpException.getStatusCode(), httpException.getStatusText()), httpException);
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
