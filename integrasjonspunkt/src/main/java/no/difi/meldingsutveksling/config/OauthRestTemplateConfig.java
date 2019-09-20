package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.auth.IdportenOidcTokenResponse;
import no.difi.meldingsutveksling.auth.OidcTokenClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserApprovalRequiredException;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import static java.util.Arrays.asList;

@Configuration
@Retryable
@EnableOAuth2Client
public class OauthRestTemplateConfig {

    private IntegrasjonspunktProperties props;
    private OidcTokenClient oidcTokenClient;

    @Autowired
    public OauthRestTemplateConfig(IntegrasjonspunktProperties props, OidcTokenClient oidcTokenClient) {
        this.props = props;
        this.oidcTokenClient = oidcTokenClient;
    }

    @Bean
    public RestOperations restTemplate() throws URISyntaxException {
        if (props.getOidc().isEnable()) {
            DefaultAccessTokenRequest atr = new DefaultAccessTokenRequest();
            BaseOAuth2ProtectedResourceDetails resource = new BaseOAuth2ProtectedResourceDetails();
            resource.setAccessTokenUri(String.valueOf(props.getOidc().getUrl().toURI()));
            resource.setScope(asList(oidcTokenClient.getCurrentScopes().split(" ")));
            resource.setClientId(props.getOidc().getClientId());

            OAuth2RestTemplate rt = new OAuth2RestTemplate(resource, new DefaultOAuth2ClientContext(atr));
            rt.setAccessTokenProvider(new OidcAccessTokenProvider());
            return rt;
        }
        return new RestTemplate();
    }

    public class OidcAccessTokenProvider implements AccessTokenProvider {

        private DefaultOAuth2AccessToken getAccessToken() {
            IdportenOidcTokenResponse oidcTokenResponse = oidcTokenClient.fetchToken();
            DefaultOAuth2AccessToken oa2at = new DefaultOAuth2AccessToken(oidcTokenResponse.getAccessToken());
            oa2at.setExpiration(Date.from(Instant.now().plusSeconds(oidcTokenResponse.getExpiresIn())));
            oa2at.setScope(Collections.singleton(oidcTokenResponse.getScope()));
            return oa2at;
        }

        @Override
        public OAuth2AccessToken obtainAccessToken(OAuth2ProtectedResourceDetails details, AccessTokenRequest parameters) throws UserRedirectRequiredException, UserApprovalRequiredException, AccessDeniedException {
            return getAccessToken();
        }

        @Override
        public boolean supportsResource(OAuth2ProtectedResourceDetails resource) {
            return false;
        }

        @Override
        public OAuth2AccessToken refreshAccessToken(OAuth2ProtectedResourceDetails resource, OAuth2RefreshToken refreshToken, AccessTokenRequest request) throws UserRedirectRequiredException {
            return getAccessToken();
        }

        @Override
        public boolean supportsRefresh(OAuth2ProtectedResourceDetails resource) {
            return false;
        }
    }
}
