package no.difi.meldingsutveksling.config;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import no.difi.move.common.oauth.Oauth2JwtAccessTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableRetry
@EnableOAuth2Client
@RequiredArgsConstructor
public class OauthRestTemplateConfig {

    private static final String CLIENT_ID_PREFIX = "MOVE_IP_";

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

    @SneakyThrows
    @Bean
    public JwtTokenClient jwtTokenClient() {
        JwtTokenConfig config = new JwtTokenConfig(
                !Strings.isNullOrEmpty(props.getOidc().getClientId()) ?
                        props.getOidc().getClientId() : CLIENT_ID_PREFIX+props.getOrg().getNumber(),
                props.getOidc().getUrl().toString(),
                props.getOidc().getAudience(),
                getCurrentScopes(),
                props.getOidc().getKeystore()
        );

        return new JwtTokenClient(config);
    }

    @Bean
    public RestOperations restTemplate(JwtTokenClient jwtTokenClient) throws URISyntaxException {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        if (props.getOidc().isEnable()) {
            DefaultAccessTokenRequest atr = new DefaultAccessTokenRequest();
            BaseOAuth2ProtectedResourceDetails resource = new BaseOAuth2ProtectedResourceDetails();
            resource.setAccessTokenUri(String.valueOf(props.getOidc().getUrl().toURI()));
            resource.setScope(getCurrentScopes());
            resource.setClientId(props.getOidc().getClientId());

            OAuth2RestTemplate rt = new OAuth2RestTemplate(resource, new DefaultOAuth2ClientContext(atr));
            rt.setRequestFactory(requestFactory);
            rt.setAccessTokenProvider(new Oauth2JwtAccessTokenProvider(jwtTokenClient));
            return rt;
        }

        return new RestTemplate(requestFactory);
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
