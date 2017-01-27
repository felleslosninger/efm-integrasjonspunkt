package no.difi.meldingsutveksling.serviceregistry.client;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * RestClient using simple http requests to manipulate services
 *
 * Example:
 * RestClient client = new RestClient("http://localhost:8080/organization");
 * String json = client.getResource("1234567);
 * Would perform HTTP GET against http://localhost:8080/organization/1234567
 * and return the HTTP Response body as a String
 */
@Component
public class RestClient {

    private final IntegrasjonspunktProperties props;
    private final RestOperations restTemplate;

    private final URI baseUrl;

    /**
     * Creates a simple Rest Client based on RestTemplate.
     *
     * @throws URISyntaxException
     */
    @Autowired
    public RestClient(IntegrasjonspunktProperties props, RestOperations restTemplate) throws MalformedURLException,
            URISyntaxException {
        this.props = props;
        this.restTemplate = restTemplate;
        this.baseUrl = new URL(props.getServiceregistryEndpoint()).toURI();
    }

    /**
     * Performs HTTP GET against baseUrl/resourcePath
     *
     * @param resourcePath which is resolved against baseUrl
     * @return response body
     */
    public String getResource(String resourcePath) {
        return getResource(resourcePath, null);
    }


    /**
     * Performs HTTP GET against baseUrl/resourcePath
     *
     * @param resourcePath which is resolved against baseUrl
     * @return response body
     */
    public String getResource(String resourcePath, String query) {
        URI uri = UriComponentsBuilder.fromUri(baseUrl).pathSegment(resourcePath).query(query).build().toUri();

        return restTemplate.getForObject(uri, String.class);
    }

    public void putResource(String resourcePath) {
        URI uri = UriComponentsBuilder.fromUri(baseUrl).pathSegment(resourcePath).build().toUri();
        restTemplate.put(uri, String.class);
    }
}
