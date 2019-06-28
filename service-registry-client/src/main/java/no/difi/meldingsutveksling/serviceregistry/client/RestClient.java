package no.difi.meldingsutveksling.serviceregistry.client;

import com.nimbusds.jose.proc.BadJWSException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.oauth.JWTDecoder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.PublicKey;
import java.util.Collections;

import static java.util.Arrays.asList;

/**
 * RestClient using simple http requests to manipulate services
 * <p>
 * Example:
 * RestClient client = new RestClient("http://localhost:8080/identifier");
 * String json = client.getResource("1234567);
 * Would perform HTTP GET against http://localhost:8080/identifier/1234567
 * and return the HTTP Response body as a String
 */
@RequiredArgsConstructor
public class RestClient {

    private final IntegrasjonspunktProperties props;
    @Getter private final RestOperations restTemplate;
    private final JWTDecoder jwtDecoder;
    private final PublicKey pk;
    private final URI baseUrl;

    /**
     * Performs HTTP GET against baseUrl/resourcePath
     *
     * @param resourcePath which is resolved against baseUrl
     * @return response body
     */
    public String getResource(String resourcePath) throws BadJWSException {
        return getResource(resourcePath, null);
    }

    /**
     * Performs HTTP GET against baseUrl/resourcePath
     *
     * @param resourcePath which is resolved against baseUrl
     * @return response body
     */
    public String getResource(String resourcePath, String query) throws BadJWSException {
        URI uri = UriComponentsBuilder.fromUri(baseUrl).pathSegment(resourcePath).query(query).build().toUri();

        if (props.getSign().isEnable()) {
            HttpHeaders headers = new HttpHeaders();
            headers.put("Accept", Collections.singletonList("application/jose, application/json"));

            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
            return jwtDecoder.getPayload(response.getBody(), pk);
        }

        return restTemplate.getForObject(uri, String.class);
    }

    public void putResource(String resourcePath) {
        URI uri = UriComponentsBuilder.fromUri(baseUrl).pathSegment(resourcePath).build().toUri();
        restTemplate.put(uri, String.class);
    }
}
