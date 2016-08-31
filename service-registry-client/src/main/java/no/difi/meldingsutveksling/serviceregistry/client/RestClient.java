package no.difi.meldingsutveksling.serviceregistry.client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * RestClient using simple http requests to manipulate services
 *
 * Example:
 * RestClient client = new RestClient("http://localhost:8080/organization");
 * String json = client.getResource("1234567);
 * Would perform HTTP GET against http://localhost:8080/organization/1234567
 * and return the HTTP Response body as a String
 */
public class RestClient {
    private final URI baseUrl;
    private final RestTemplate template = new RestTemplate();

    /**
     * Creates a simple Rest Client based on RestTemplate.
     *
     * @param baseUrl for instance http://localhost:8080
     * @throws URISyntaxException
     */
    public RestClient(String baseUrl) throws URISyntaxException {
        this.baseUrl = new URI(baseUrl);
    }

    /**
     * Performs HTTP GET against baseUrl/resourcePath
     *
     * @param resourcePath which is resolved against baseUrl
     * @return response body
     */
    public String getResource(String resourcePath) {
        URI uri = UriComponentsBuilder.fromUri(baseUrl).pathSegment(resourcePath).build().toUri();

        final ResponseEntity<String> entity = template.getForEntity(uri, String.class);
        return entity.getBody();
    }

    public void putResource(String resourcePath) {
        URI uri = baseUrl.resolve(resourcePath);
        template.put(uri, String.class);
    }
}
