package no.difi.meldingsutveksling.altinnv3.token;

import no.difi.move.common.oauth.JwtTokenClient;
import no.difi.move.common.oauth.JwtTokenConfig;
import no.difi.move.common.oauth.JwtTokenInput;

import java.util.ArrayList;
import java.util.List;

public class MaskinportenTokenService implements TokenService {

    @Override
    public String fetchToken(TokenConfig tokenConfig, List<String> scopes) {
        var jwtTokenConfig = new JwtTokenConfig(
            tokenConfig.oidc().getClientId(),
            tokenConfig.oidc().getUrl().toString(),
            tokenConfig.oidc().getAudience(),
            new ArrayList<>(),
            tokenConfig.oidc().getKeystore()
        );
        var jtc = new JwtTokenClient(jwtTokenConfig);
        var jti = new JwtTokenInput().setClientId(tokenConfig.oidc().getClientId()).setScopes(scopes);
        return jtc.fetchToken(jti).getAccessToken();
    }

}
