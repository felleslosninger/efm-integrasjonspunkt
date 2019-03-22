package no.difi.meldingsutveksling.ks.svarinn;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(name="difi.move.feature.enableDPO", havingValue = "true")
@EnableConfigurationProperties(IntegrasjonspunktProperties.class)
public class SvarInnBeans {
    @Bean
    public RestTemplate svarInnRestTemplate(IntegrasjonspunktProperties properties) {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        builder = builder.rootUri(properties.getFiks().getInn().getBaseUrl());
        builder = builder.basicAuthorization(properties.getFiks().getInn().getUsername(), properties.getFiks().getInn().getPassword());
        return builder.build();
    }

    @Bean
    SvarInnService svarInnService(SvarInnClient svarInnClient,
                                  SvarInnFileDecryptor svarInnFileDecryptor,
                                  SvarInnUnzipper svarInnUnzipper,
                                  IntegrasjonspunktProperties properties) {
        return new SvarInnService(svarInnClient, svarInnFileDecryptor, svarInnUnzipper, properties);
    }

    @Bean
    public SvarInnClient svarInnClient(RestTemplate svarInnRestTemplate) {
        return new SvarInnClient(svarInnRestTemplate);
    }

    @Bean
    public SvarInnFileDecryptor svarInnFileDecryptor(IntegrasjonspunktProperties properties) {
        return new SvarInnFileDecryptor(properties.getFiks().getKeystore());
    }

    @Bean
    public SvarInnUnzipper svarInnUnzipper() {
        return new SvarInnUnzipper();
    }

}
