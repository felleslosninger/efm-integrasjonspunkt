package no.difi.meldingsutveksling.altinnv3.token;

import no.difi.meldingsutveksling.config.AuthenticationType;
import no.difi.move.common.oauth.JwtTokenAdditionalClaims;
import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import no.difi.move.common.oauth.JwtTokenInput;

import java.util.List;

public class MaskinportenTokenService implements TokenService {

    @Override
    public String fetchToken(TokenConfig tokenConfig, List<String> scopes, JwtTokenAdditionalClaims additionalClaims) {

        var jwtTokenConfig = getJwtTokenConfig(tokenConfig);

        var jtc = new JwtTokenClient(jwtTokenConfig);
        var jti = new JwtTokenInput().setClientId(tokenConfig.oidc().getClientId()).setScopes(scopes);
        return jtc.fetchToken(jti, additionalClaims).getAccessToken();
    }

    private JwtTokenConfig getJwtTokenConfig(TokenConfig tokenConfig) {

        var jwtTokenConfig = new JwtTokenConfig()
            .setClientId(tokenConfig.oidc().getClientId())
            .setTokenUri(tokenConfig.oidc().getUrl().toString())
            .setAudience(tokenConfig.oidc().getAudience());


        if (AuthenticationType.JWK.equals(tokenConfig.oidc().getAuthenticationType())){
            jwtTokenConfig
                .setJwk(tokenConfig.oidc().getJwk())
                .setAuthenticationType(no.difi.move.common.oauth.AuthenticationType.JWK);
        } else if(AuthenticationType.CERTIFICATE.equals(tokenConfig.oidc().getAuthenticationType())) {
            jwtTokenConfig
                .setKeystore(tokenConfig.oidc().getKeystore())
                .setAuthenticationType(no.difi.move.common.oauth.AuthenticationType.CERTIFICATE);
        } else {
            throw new IllegalArgumentException("Unsupported authentication type: " + tokenConfig.oidc().getAuthenticationType());
        }

        return jwtTokenConfig;
    }

}
