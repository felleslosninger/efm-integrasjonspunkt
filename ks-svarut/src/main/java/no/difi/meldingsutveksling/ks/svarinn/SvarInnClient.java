package no.difi.meldingsutveksling.ks.svarinn;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

public class SvarInnClient {
    public List<Forsendelse> checkForNewMessages() {
        RestTemplate restTemplate = new RestTemplate(createRequestFactory());

        final List<Forsendelse> forsendelser = Arrays.asList(restTemplate.getForObject("https://test.svarut.ks.no/tjenester/svarinn/mottaker/hentNyeForsendelser", Forsendelse[].class));
//        final String forsendelser = restTemplate.getForObject("https://test.svarut.ks.no/tjenester/svarinn/mottaker/hentNyeForsendelser", String.class);

        return forsendelser;

    }

    public void receive() {

    }

    private HttpComponentsClientHttpRequestFactory createRequestFactory() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("username", "password"));

        final CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

        final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return requestFactory;
    }
}
