package no.difi.meldingsutveksling.serviceregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.proc.BadJWSException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.CacheConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.IdentifierResource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRegistryClient {

    private final RestClient client;
    private final SasKeyRepository sasKeyRepository;
    private final ObjectMapper objectMapper;
    private final IntegrasjonspunktProperties props;

    @Cacheable(CacheConfig.CACHE_LOAD_IDENTIFIER_RESOURCE)
    public IdentifierResource loadIdentifierResource(SRParameter parameter) throws ServiceRegistryLookupException {
        String identifierResourceString = getIdentifierResourceString(parameter);

        try {
            return objectMapper.readValue(identifierResourceString, IdentifierResource.class);
        } catch (IOException e) {
            throw new ServiceRegistryLookupException(
                    String.format("Parsing response as a IdentifierResource JSON object failed. Content is: %s", identifierResourceString)
                    , e);
        }
    }

    private String getIdentifierResourceString(SRParameter parameter) throws ServiceRegistryLookupException {
        if(!props.getFeature().isEnableDsfPrintLookup()) {
            //Default value in SR is true. If you want to avoid DSF lookup set this property to false.
            parameter.setPrint(props.getFeature().isEnableDsfPrintLookup());
        }
        try {
            return client.getResource(parameter.getUrlTemplate(), parameter.getUrlVariables());
        } catch (HttpClientErrorException httpException) {
            if (httpException.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NotFoundInServiceRegistryException(parameter);
            }
            byte[] errorBody = httpException.getResponseBodyAsByteArray();
            try {
                ErrorResponse error = objectMapper.readValue(errorBody, ErrorResponse.class);
                throw new ServiceRegistryLookupException(String.format("Caught exception when looking up service record with parameter %s, http status %s (%s): %s",
                        parameter, httpException.getStatusCode(), httpException.getStatusText(), error.getErrorDescription()), httpException);
            } catch (IOException e) {
                log.warn("Could not parse error response from service registry");
                throw new ServiceRegistryLookupException(String.format("Caught exception when looking up service record with parameter %s, http status: %s (%s)",
                        parameter, httpException.getStatusCode(), httpException.getStatusText()), httpException);
            }
        } catch (BadJWSException e) {
            log.error("Bad signature in service record response", e);
            throw new ServiceRegistryLookupException("Bad signature in response from service registry", e);
        }
    }

    @Cacheable(CacheConfig.CACHE_SR_VIRKSERT)
    public String getCertificate(String identifier) throws ServiceRegistryLookupException {
        try {
            Map<String, String> params = Maps.newHashMap();
            params.put("identifier", identifier);
            return client.getResource("virksert/{identifier}", params);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND ||
                e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                String errorDescription = JsonPath.read(e.getResponseBodyAsString(), "$.error_description");
                throw new ServiceRegistryLookupException(errorDescription);
            }
            throw new NextMoveRuntimeException("Error looking up certificate in service registry for identifier " + identifier, e);
        } catch (BadJWSException e) {
            throw new NextMoveRuntimeException("Bad signature in response from service registry", e);
        }
    }

    @Cacheable(value = CacheConfig.CACHE_GET_SAS_KEY, sync = true)
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
