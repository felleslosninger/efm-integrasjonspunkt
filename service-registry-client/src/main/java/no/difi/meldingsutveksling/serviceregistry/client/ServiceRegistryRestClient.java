package no.difi.meldingsutveksling.serviceregistry.client;

import com.nimbusds.jose.proc.BadJWSException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.oauth.JWTDecoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

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
public class ServiceRegistryRestClient {

    private final IntegrasjonspunktProperties props;
    @Getter private final RestClient restClient;
    private final JWTDecoder jwtDecoder;
    private final URI baseUrl;

    /**
     * Performs HTTP GET against baseUrl/resourcePath
     *
     * @param resourcePath which is resolved against baseUrl
     * @return response body
     */
    public String getResource(String resourcePath) throws BadJWSException {
        return getResource(resourcePath, Collections.emptyMap());
    }

    public String getResource(String urlTemplate, Map<String, String> urlVariables) throws BadJWSException {
        UriTemplate uriTemplate = new UriTemplate(baseUrl.toString().concat("/").concat(urlTemplate));
        var uri = uriTemplate.expand(urlVariables);
        if (props.getSign().isEnable()) {
            var body = restClient.get().uri(uri).header("Accept", "application/jose", "application/json").retrieve().toEntity(String.class).getBody();
            return jwtDecoder.getPayload(body, props.getSign().getJwkUrl());
        }
        return restClient.get().uri(uri).retrieve().toEntity(String.class).getBody();
    }

//    public void putResource(String resourcePath) {
//        URI uri = UriComponentsBuilder.fromUri(baseUrl).pathSegment(resourcePath).build().toUri();
//        restTemplate.put(uri, String.class);
//    }

}
