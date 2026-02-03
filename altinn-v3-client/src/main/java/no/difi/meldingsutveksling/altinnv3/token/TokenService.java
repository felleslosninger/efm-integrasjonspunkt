package no.difi.meldingsutveksling.altinnv3.token;

import no.difi.move.common.oauth.JwtTokenAdditionalClaims;

import java.util.List;

public interface TokenService {
    String fetchToken(TokenConfig tokenConfig, List<String> scopes, JwtTokenAdditionalClaims additionalClaims);
}
