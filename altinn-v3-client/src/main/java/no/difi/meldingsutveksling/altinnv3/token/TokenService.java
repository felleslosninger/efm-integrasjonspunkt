package no.difi.meldingsutveksling.altinnv3.token;

import java.util.List;

public interface TokenService {
    String fetchToken(TokenConfig tokenConfig, List<String> scopes, AdditionalClaims additionalClaims);
}
