package no.difi.meldingsutveksling.oauth2;

import no.difi.meldingsutveksling.observability.MetricsRestClientInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Oauth2RestTemplateConfig {

    private final MetricsRestClientInterceptor metricsRestClientInterceptor;
    private final MaskinportenTokenInterceptor maskinportenTokenInterceptor;

    public Oauth2RestTemplateConfig(MetricsRestClientInterceptor metricsRestClientInterceptor,
                                    MaskinportenTokenInterceptor maskinportenTokenInterceptor) {
        this.metricsRestClientInterceptor = metricsRestClientInterceptor;
        this.maskinportenTokenInterceptor = maskinportenTokenInterceptor;
    }

    @Bean
    @ConditionalOnProperty(value = "difi.move.oidc.enable", havingValue = "false")
    public RestClient restTemplate() {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        RestTemplate rt = new RestTemplate(requestFactory);

        var rc = RestClient
            .builder(rt)
            .requestInterceptor(metricsRestClientInterceptor)
            .build();

        return rc;

    }

    @Bean(name = "restTemplate")
    @ConditionalOnProperty(value = "difi.move.oidc.enable", havingValue = "true")
    public RestClient oauthRestTemplate() {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        RestTemplate rt = new RestTemplate(requestFactory);

        var rc = RestClient
            .builder(rt)
            .requestInterceptor(metricsRestClientInterceptor)
            .requestInterceptor(maskinportenTokenInterceptor)
            .build();

        return rc;

    }

}
