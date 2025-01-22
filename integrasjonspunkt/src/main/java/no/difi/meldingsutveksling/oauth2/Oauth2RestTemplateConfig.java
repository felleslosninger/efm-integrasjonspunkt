package no.difi.meldingsutveksling.oauth2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Oauth2RestTemplateConfig {

    private final MaskinportenTokenInterceptor interceptor;

    public Oauth2RestTemplateConfig(MaskinportenTokenInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Bean
    @ConditionalOnProperty(value = "difi.move.oidc.enable", havingValue = "false")
    public RestClient restTemplate() {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        RestTemplate rt = new RestTemplate(requestFactory);

        var rc = RestClient.builder(rt).build();

        // FIXME kanskje metrics ikke fungerer lenger, sjekk hvilke metrics som ble oppdatert tidligere
        //import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
        //import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer;
        // sjekk : https://dzone.com/articles/spring-boot-32-replace-your-resttemplate-with-rest
        //metricsRestTemplateCustomizer.customize(rc);

        return rc;
    }

    @Bean(name = "restTemplate")
    @ConditionalOnProperty(value = "difi.move.oidc.enable", havingValue = "true")
    public RestClient oauthRestTemplate() {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        RestTemplate rt = new RestTemplate(requestFactory);

        var rc = RestClient.builder(rt).requestInterceptor(interceptor).build();

        // FIXME kanskje metrics ikke fungerer lenger, sjekk hvilke metrics som ble oppdatert tidligere
        //import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
        //import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer;
        // sjekk : https://dzone.com/articles/spring-boot-32-replace-your-resttemplate-with-rest
        //metricsRestTemplateCustomizer.customize(rc);

        return rc;
    }

}
