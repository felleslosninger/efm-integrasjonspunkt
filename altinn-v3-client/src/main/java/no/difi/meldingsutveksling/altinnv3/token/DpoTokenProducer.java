package no.difi.meldingsutveksling.altinnv3.token;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.AltinnSystemUser;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.oauth.JwtTokenAdditionalClaims;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DpoTokenProducer {

    private final IntegrasjonspunktProperties properties;
    private final TokenService tokenService;
    private final TokenExchangeService tokenExchangeService;

    // FIXME : should we refactor this back so it implements TokenProducer like the other token producers?
    //  step 1 - rename "produceToken" method to "produceSystemUserToken"
    //  step 2 - implements TokenProducer and add produceToken that just produce regular Altinn token
    //  step 3 - a system user null check should throw execption "produceSystemUserToken"

    @Cacheable(cacheNames = {"altinn.getDpoToken"})
    public String produceToken(AltinnSystemUser systemUser, List<String> scopes) {

        var config = new TokenConfig(properties.getDpo().getOidc(), properties.getDpo().getAltinnTokenExchangeUrl());
        var authorizationClaims = (systemUser == null) ? new JwtTokenAdditionalClaims() : ClaimsFactory.getAuthorizationClaims(systemUser);

        String maskinportenToken = tokenService.fetchToken(config, scopes, authorizationClaims);

        // altinn token har levetid på 1800 sekunder (30 min), vs 120 sekunder (2 min) for maskinporten token
        var altinnToken = tokenExchangeService.exchangeToken(maskinportenToken, config.exchangeUrl());

        return altinnToken;
    }

}
