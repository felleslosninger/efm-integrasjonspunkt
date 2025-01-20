package no.difi.meldingsutveksling.config;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import no.difi.move.common.oauth.Oauth2JwtAccessTokenProvider;
import org.springframework.boot.actuate.metrics.web.client.ObservationRestClientCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableRetry
@EnableOAuth2Client
@RequiredArgsConstructor
public class OauthRestTemplateConfig {

    private static final String SCOPE_DPO = "move/dpo.read";
    private static final String SCOPE_DPE = "move/dpe.read";
    private static final String SCOPE_DPV = "move/dpv.read";
    private static final String SCOPE_DPF = "move/dpf.read";
    private static final String SCOPE_DPFIO = "ks:fiks";
    private static final List<String> SCOPES_DPI = Arrays.asList("move/dpi.read",
            "global/kontaktinformasjon.read",
            "global/sikkerdigitalpost.read",
            "global/varslingsstatus.read",
            "global/sertifikat.read",
            "global/navn.read",
            "global/postadresse.read");

    private final IntegrasjonspunktProperties props;
    private final ObservationRestClientCustomizer metricsRestTemplateCustomizer;

    @SneakyThrows
    @Bean
    @ConditionalOnProperty(value = "difi.move.oidc.enable", havingValue = "true")
    public JwtTokenClient jwtTokenClient() {
        JwtTokenConfig config = new JwtTokenConfig(
                props.getOidc().getClientId(),
                props.getOidc().getUrl().toString(),
                props.getOidc().getAudience(),
                getCurrentScopes(),
                props.getOidc().getKeystore()
        );

        return new JwtTokenClient(config);
    }

    @Bean
    @ConditionalOnProperty(value = "difi.move.oidc.enable", havingValue = "false")
    public RestOperations restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        RestTemplate rt = new RestTemplate(requestFactory);
        // FIXME client blir ikke returnert, kanskje metrics ikke fungerer lenger?
        // sjekk : https://dzone.com/articles/spring-boot-32-replace-your-resttemplate-with-rest
        var rc = RestClient.builder(rt);
        metricsRestTemplateCustomizer.customize(rc);
        return rt;
    }

    @Bean(name = "restTemplate")
    @ConditionalOnProperty(value = "difi.move.oidc.enable", havingValue = "true")
    public RestOperations oauthRestTemplate(JwtTokenClient jwtTokenClient) throws URISyntaxException {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        BaseOAuth2ProtectedResourceDetails resource = new BaseOAuth2ProtectedResourceDetails();
        resource.setAccessTokenUri(String.valueOf(props.getOidc().getUrl().toURI()));
        resource.setScope(getCurrentScopes());
        resource.setClientId(props.getOidc().getClientId());

        OAuth2RestTemplate rt = new SyncedOauth2RestTemplate(resource);
        rt.setRequestFactory(requestFactory);
        rt.setAccessTokenProvider(new Oauth2JwtAccessTokenProvider(jwtTokenClient));
        rt.setUriTemplateHandler(new DefaultUriBuilderFactory());

        // FIXME client blir ikke returnert, kanskje metrics ikke fungerer lenger?
        // sjekk : https://dzone.com/articles/spring-boot-32-replace-your-resttemplate-with-rest
        var rc = RestClient.builder(rt);
        metricsRestTemplateCustomizer.customize(rc);

        return rt;
    }

    private ArrayList<String> getCurrentScopes() {

        ArrayList<String> scopeList = new ArrayList<>();
        if (props.getFeature().isEnableDPO()) {
            scopeList.add(SCOPE_DPO);
        }
        if (props.getFeature().isEnableDPE()) {
            scopeList.add(SCOPE_DPE);
        }
        if (props.getFeature().isEnableDPV()) {
            scopeList.add(SCOPE_DPV);
        }
        if (props.getFeature().isEnableDPF()) {
            scopeList.add(SCOPE_DPF);
        }
        if (props.getFeature().isEnableDPFIO()) {
            scopeList.add(SCOPE_DPFIO);
        }
        if (props.getFeature().isEnableDPI()) {
            scopeList.addAll(SCOPES_DPI);
        }

        return scopeList;
    }
}
