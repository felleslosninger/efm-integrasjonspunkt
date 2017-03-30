package no.difi.meldingsutveksling.serviceregistry.client;

import com.nimbusds.jose.proc.BadJWSException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.oauth.JWTDecoder;
import no.difi.move.common.oauth.KeystoreHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateException;

import static java.util.Arrays.asList;

/**
 * RestClient using simple http requests to manipulate services
 *
 * Example:
 * RestClient client = new RestClient("http://localhost:8080/identifier");
 * String json = client.getResource("1234567);
 * Would perform HTTP GET against http://localhost:8080/identifier/1234567
 * and return the HTTP Response body as a String
 */
@Component
public class RestClient {

    private final IntegrasjonspunktProperties props;
    private final RestOperations restTemplate;
    private final JWTDecoder jwtDecoder;

    private final URI baseUrl;

    /**
     * Creates a simple Rest Client based on RestTemplate.
     *
     * @throws URISyntaxException
     */
    @Autowired
    public RestClient(IntegrasjonspunktProperties props,
                      RestOperations restTemplate,
                      @Qualifier("signingKeystoreHelper") KeystoreHelper keystoreHelper)
            throws MalformedURLException, URISyntaxException, CertificateException {
        this.props = props;
        this.restTemplate = restTemplate;
        this.jwtDecoder = new JWTDecoder(keystoreHelper);
        this.baseUrl = new URL(props.getServiceregistryEndpoint()).toURI();
    }

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
            headers.put("Accept", asList("application/jose"));

            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
            return jwtDecoder.getPayload(response.getBody());
        }

        return restTemplate.getForObject(uri, String.class);
    }

    public void putResource(String resourcePath) {
        URI uri = UriComponentsBuilder.fromUri(baseUrl).pathSegment(resourcePath).build().toUri();
        restTemplate.put(uri, String.class);
    }
}
