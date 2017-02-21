package no.difi.meldingsutveksling.ks.svarinn;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SvarInnBeans {
    @Bean
    public RestTemplate svarInnRestTemplate() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        builder.basicAuthorization("username", "password");
        return builder.build();
    }

    @Bean
    public SvarInnClient svarInnClient(RestTemplate svarInnRestTemplate) {
        return new SvarInnClient(svarInnRestTemplate);
    }

}
