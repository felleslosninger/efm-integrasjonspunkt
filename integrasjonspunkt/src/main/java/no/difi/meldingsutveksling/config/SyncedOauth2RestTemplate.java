package no.difi.meldingsutveksling.config;

import lombok.Synchronized;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class SyncedOauth2RestTemplate extends OAuth2RestTemplate {

    public SyncedOauth2RestTemplate(OAuth2ProtectedResourceDetails resource) {
        super(resource);
    }

    @Override
    @Synchronized
    public OAuth2AccessToken getAccessToken() throws UserRedirectRequiredException {
        return super.getAccessToken();
    }
}
