package no.difi.meldingsutveksling.oauth2;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
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
    public RestClient restTemplate(IntegrasjonspunktProperties properties) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getOidc().getConnectTimeout());
        requestFactory.setReadTimeout(properties.getOidc().getReadTimeout());

        RestTemplate rt = new RestTemplate(requestFactory);

        return RestClient
            .builder(rt)
            .requestInterceptor(metricsRestClientInterceptor)
            .build();
    }

    @Bean(name = "restTemplate")
    @ConditionalOnProperty(value = "difi.move.oidc.enable", havingValue = "true")
    public RestClient oauthRestTemplate(IntegrasjonspunktProperties properties) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getOidc().getConnectTimeout());
        requestFactory.setReadTimeout(properties.getOidc().getReadTimeout());

        RestTemplate rt = new RestTemplate(requestFactory);

        return RestClient
            .builder(rt)
            .requestInterceptor(metricsRestClientInterceptor)
            .requestInterceptor(maskinportenTokenInterceptor)
            .build();
    }
}
